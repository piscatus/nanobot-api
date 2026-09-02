package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BonusesDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.TimeUtil;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FishServicesImpl implements FishServices {

  /**
   * A catch debits the guild reserve twice the creature's value, because
   * selling the creature back pays the user twice that value.
   */
  private static final BigInteger RESERVE_MULTIPLIER = BigInteger.valueOf(2L);

  private static final int DEFAULT_FISHING_FREQUENCY_MINUTES = 15;

  public final CoreServices coreServices;

  public final AnglersService anglersService;

  public final CurrenciesService currenciesService;

  public final GuildWalletsService guildWalletsService;

  public final LeaderboardsService leaderboardsService;

  public final CreaturesService creaturesService;

  public final UserItemsService userItemsService;

  @Autowired
  private TransferExecutorService transferExecutorService;

  public FishServicesImpl(
    AnglersService anglersService,
    CoreServices coreServices,
    CurrenciesService currenciesService,
    GuildWalletsService guildWalletsService,
    LeaderboardsService leaderboardsService,
    CreaturesService creaturesService,
    UserItemsService userItemsService
  ) {
    this.anglersService = anglersService;
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.guildWalletsService = guildWalletsService;
    this.leaderboardsService = leaderboardsService;
    this.creaturesService = creaturesService;
    this.userItemsService = userItemsService;
  }

  @Override
  public TransferResponseDto fish(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_FISH, requestDto);

      TransferResponseDto transferResponseDto = new TransferResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          transferResponseDto::setGuildConfigurations
        )
      ) {
        return transferResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          transferResponseDto::setUserDetails,
          true
        )
      ) {
        return transferResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_FISH,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      // setup command map for use later for errors
      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setBonuses(new BonusesDto().getBonuses());

      String roleError = findRoleError(requestDto, transferResponseDto);
      if (roleError != null) {
        transferResponseDto.setErrorMessage(roleError);
        return transferResponseDto;
      }

      final String botUserId = System.getenv("BOT_USER_ID");
      final String emptyReservesError = formatEmptyReservesError(
        requestDto.getUserId(),
        botUserId,
        commandMap
      );

      // Server Balances
      List<GuildWalletsEntity> guildWalletsList =
        guildWalletsService.getGuildsWallets(requestDto.getGuildId());
      if (guildWalletsList.isEmpty()) {
        transferResponseDto.setErrorMessage(emptyReservesError);
        return transferResponseDto;
      }
      GuildWalletsDto guildWalletDto = new GuildWalletsDto(
        guildWalletsList.get(0)
      );
      List<WalletDto> guildWallet = guildWalletDto.getWallets();

      // Creatures whose currency is still enabled, and the priciest catch per ticker
      List<CreatureDto> stockedCreatures = findStockedCreatures(
        creaturesService.getCreatures(),
        transferResponseDto.getCurrencies()
      );
      Map<String, BigInteger> maxCreatureValuesByTicker =
        findMaxCreatureValuesByTicker(stockedCreatures);

      Set<String> affordableTickers = findAffordableTickers(
        guildWallet,
        maxCreatureValuesByTicker
      );

      if (affordableTickers.isEmpty()) {
        transferResponseDto.setErrorMessage(emptyReservesError);
        return transferResponseDto;
      }

      // Optional currency filter, so a user can target one ticker's creatures
      String requestedTicker = normalizeTicker(requestDto.getTicker());
      if (requestedTicker != null) {
        String tickerError = findRequestedTickerError(
          requestedTicker,
          maxCreatureValuesByTicker.keySet(),
          affordableTickers,
          requestDto.getUserId(),
          botUserId,
          commandMap,
          transferResponseDto.getCurrencies()
        );
        if (tickerError != null) {
          transferResponseDto.setErrorMessage(tickerError);
          return transferResponseDto;
        }
      }

      List<CreatureDto> catchPool = stockedCreatures
        .stream()
        .filter(creature -> affordableTickers.contains(creature.getTicker()))
        .filter(creature ->
          requestedTicker == null || requestedTicker.equals(creature.getTicker())
        )
        .toList();

      // Declare users item set
      Set<UserItemsEntity> currentUserItemQuantities = new HashSet<>();

      // Fetch sender items
      List<UserItemsEntity> senderItemsList = userItemsService.getUsersItems(
        requestDto.getUserId()
      );

      if (!senderItemsList.isEmpty()) {
        currentUserItemQuantities.add(senderItemsList.get(0));

        String capacityError = findCapacityError(
          senderItemsList.get(0),
          catchPool,
          requestDto.getUserId(),
          commandMap
        );
        if (capacityError != null) {
          transferResponseDto.setErrorMessage(capacityError);
          return transferResponseDto;
        }
      }

      // Call logCurrentItems() method before fishing
      LoggingUtil.logCurrentItems(currentUserItemQuantities);

      String cooldownError = findCooldownError(
        requestDto,
        transferResponseDto,
        commandMap
      );
      if (cooldownError != null) {
        transferResponseDto.setErrorMessage(cooldownError);
        return transferResponseDto;
      }

      CreatureDto creature = selectWeightedCreature(catchPool);

      if (creature == null) {
        transferResponseDto.setErrorMessage(
          "<@" + requestDto.getUserId() + "> crashed their fishing boat."
        );
        return transferResponseDto;
      }

      transferResponseDto.setConfirmation(true);
      transferResponseDto.setPrimaryTransfer(
        new TransferDto(
          Collections.singletonList(
            new WalletDto(
              creature.getTicker(),
              (new BigInteger(creature.getValue()).multiply(
                  RESERVE_MULTIPLIER
                )).toString(),
              true
            )
          ),
          new ArrayList<>()
        )
      );
      transferResponseDto.setSecondaryTransfer(
        new TransferDto(
          new ArrayList<>(),
          Collections.singletonList(
            new ItemDto(creature.getName().toUpperCase(), 1, true)
          )
        )
      );

      TransferResponseDto transfer = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_FISH,
        requestDto.getGuildId(),
        requestDto.getChannelId(),
        botUserId,
        "0",
        Collections.singletonList("0"),
        Collections.singletonList(requestDto.getUserId()),
        transferResponseDto
      );

      if (transfer.getErrorMessage() != null) {
        return transfer;
      }

      //set the fishing time
      anglersService.updateOrCreateAngler(
        requestDto.getGuildId(),
        requestDto.getUserId()
      );

      // Leaderboard increment
      leaderboardsService.incrementOrCreateLeaderboard(
        requestDto.getGuildId(),
        requestDto.getUserId(),
        creature
      );

      return transfer;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_FISH, e);
      return new TransferResponseDto(
        "<@" +
        requestDto.getUserId() +
        "> crashed their fishing boat. " +
        Constants.unknownError
      );
    }
  }

  /**
   * Error when the guild gates fishing behind a role the user does not hold and
   * cannot bypass, otherwise null.
   */
  private String findRoleError(
    RequestDto requestDto,
    TransferResponseDto transferResponseDto
  ) {
    String requiredFishingRole = transferResponseDto
      .getGuildConfigurations()
      .getFishingRole();

    if (
      requiredFishingRole == null ||
      requiredFishingRole.equals("") ||
      requiredFishingRole.equals("0") ||
      requestDto.getUserRoles().contains(requiredFishingRole)
    ) {
      return null;
    }

    List<String> requiredFishingBypassRoles = transferResponseDto
      .getGuildConfigurations()
      .getFishingBypassRoles();

    if (
      requiredFishingBypassRoles != null &&
      requestDto
        .getUserRoles()
        .stream()
        .anyMatch(requiredFishingBypassRoles::contains)
    ) {
      return null;
    }

    String errorMessage =
      "<@" +
      requestDto.getUserId() +
      "> does not currently hold the required role (<@&" +
      requiredFishingRole +
      ">) for fishing.";

    String requiredFishingRoleMessage = transferResponseDto
      .getGuildConfigurations()
      .getFishingError();

    if (
      requiredFishingRoleMessage != null &&
      !requiredFishingRoleMessage.isEmpty()
    ) {
      errorMessage += "\n\n**Server Message:** " + requiredFishingRoleMessage;
    }

    return errorMessage;
  }

  /** Creatures the bot can hand out at all, i.e. whose currency is enabled. */
  private List<CreatureDto> findStockedCreatures(
    List<CreatureEntity> creatureEntities,
    List<CurrencyDto> currencies
  ) {
    Map<String, Boolean> currencyEnabledMap = currencies
      .stream()
      .collect(
        Collectors.toMap(CurrencyDto::getTicker, CurrencyDto::getEnabled)
      );

    return creatureEntities
      .stream()
      .filter(creature ->
        Boolean.TRUE.equals(currencyEnabledMap.get(creature.getTicker()))
      )
      .map(CreatureDto::new)
      .toList();
  }

  private Map<String, BigInteger> findMaxCreatureValuesByTicker(
    List<CreatureDto> creatures
  ) {
    Map<String, BigInteger> maxCreatureValuesByTicker = new HashMap<>();
    for (CreatureDto creature : creatures) {
      maxCreatureValuesByTicker.merge(
        creature.getTicker(),
        new BigInteger(creature.getValue()),
        BigInteger::max
      );
    }
    return maxCreatureValuesByTicker;
  }

  /**
   * Tickers whose guild reserve covers the priciest creature on that ticker.
   * The reserve must cover twice the value, matching what the catch debits.
   */
  private Set<String> findAffordableTickers(
    List<WalletDto> guildWallet,
    Map<String, BigInteger> maxCreatureValuesByTicker
  ) {
    Set<String> affordableTickers = new HashSet<>();

    for (WalletDto wallet : guildWallet) {
      BigInteger maxCreatureValue = maxCreatureValuesByTicker.get(
        wallet.getTicker()
      );

      if (maxCreatureValue == null) {
        continue;
      }

      BigInteger walletBalance = new BigInteger(wallet.getRaw());
      if (
        walletBalance.compareTo(maxCreatureValue.multiply(RESERVE_MULTIPLIER)) >=
        0
      ) {
        affordableTickers.add(wallet.getTicker());
      }
    }

    return affordableTickers;
  }

  private String normalizeTicker(String ticker) {
    if (ticker == null || ticker.isBlank()) {
      return null;
    }
    return ticker.trim().toUpperCase();
  }

  /**
   * Error when the user asked for a currency this server cannot fish right now,
   * either because nothing has been stocked for it or because the reserve is
   * too low, otherwise null.
   */
  private String findRequestedTickerError(
    String requestedTicker,
    Set<String> stockedTickers,
    Set<String> affordableTickers,
    String userId,
    String botUserId,
    Map<String, String> commandMap,
    List<CurrencyDto> currencies
  ) {
    if (affordableTickers.contains(requestedTicker)) {
      return null;
    }

    String requested = formatCurrencyLabel(requestedTicker, currencies);
    String available = formatCurrencyLabels(affordableTickers, currencies);
    String fishAgain =
      "Run </" +
      Constants.COMMAND_NAME_FISH +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_FISH) +
      "> without a currency to fish for anything!";

    if (!stockedTickers.contains(requestedTicker)) {
      return (
        "<@" +
        userId +
        "> cannot fish for " +
        requested +
        " creatures because none have been added for that currency yet.\n" +
        "Available currencies for fishing in this server: " +
        available +
        "\n" +
        fishAgain
      );
    }

    return (
      "<@" +
      userId +
      "> cannot fish for " +
      requested +
      " creatures because this server's </" +
      Constants.COMMAND_NAME_RESERVES +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_RESERVES) +
      "> for that currency does not meet the minimum requirement.\n" +
      "Available currencies for fishing in this server: " +
      available +
      "\n" +
      "Any user can </" +
      Constants.COMMAND_NAME_GIFT +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_GIFT) +
      "> <@" +
      botUserId +
      "> from within a server to contribute to that server's </" +
      Constants.COMMAND_NAME_RESERVES +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_RESERVES) +
      ">!\n" +
      fishAgain
    );
  }

  /** A currency rendered as "emoji Name [TICKER]", falling back to the ticker. */
  private String formatCurrencyLabel(
    String ticker,
    List<CurrencyDto> currencies
  ) {
    return currencies
      .stream()
      .filter(currency -> ticker.equals(currency.getTicker()))
      .findFirst()
      .map(currency -> {
        String label = currency.getName() + " [" + currency.getTicker() + "]";
        return currency.getEmoji() == null || currency.getEmoji().isBlank()
          ? label
          : currency.getEmoji() + " " + label;
      })
      .orElse(ticker);
  }

  private String formatCurrencyLabels(
    Collection<String> tickers,
    List<CurrencyDto> currencies
  ) {
    return tickers
      .stream()
      .sorted(Comparator.naturalOrder())
      .map(ticker -> formatCurrencyLabel(ticker, currencies))
      .collect(Collectors.joining(", "));
  }

  private String formatEmptyReservesError(
    String userId,
    String botUserId,
    Map<String, String> commandMap
  ) {
    return (
      "<@" +
      userId +
      "> cannot fish because the server balance does not meet the minimum requirement.\n" +
      "Any user can </" +
      Constants.COMMAND_NAME_GIFT +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_GIFT) +
      "> <@" +
      botUserId +
      "> from within a server to contribute to that server's fishing </" +
      Constants.COMMAND_NAME_RESERVES +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_RESERVES) +
      ">!\n" +
      "See </" +
      Constants.COMMAND_NAME_HELP +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_HELP) +
      "> for more information!"
    );
  }

  /**
   * Error when the user already holds a full stack of something they could
   * catch, otherwise null. Only the creatures still in the pool are checked, so
   * a full stack of one currency's creature does not block another currency.
   */
  private String findCapacityError(
    UserItemsEntity senderItems,
    List<CreatureDto> catchPool,
    String userId,
    Map<String, String> commandMap
  ) {
    if (senderItems.getItems() == null) {
      return null;
    }

    for (ItemDto userItem : senderItems.getItems()) {
      for (CreatureDto creature : catchPool) {
        if (
          userItem.getName().equalsIgnoreCase(creature.getName()) &&
          userItem.getQuantity() >= creature.getCapacity()
        ) {
          return (
            "<@" +
            userId +
            "> has one or more </" +
            Constants.COMMAND_NAME_INVENTORY +
            ":" +
            commandMap.get(Constants.COMMAND_NAME_INVENTORY) +
            "> items at or above maximum capacity!" +
            "\n" +
            "Convert inventory items into currencies with </" +
            Constants.COMMAND_NAME_SELL +
            ":" +
            commandMap.get(Constants.COMMAND_NAME_SELL) +
            ">!"
          );
        }
      }
    }
    return null;
  }

  /** Error when the user is still resting from their last catch, otherwise null. */
  private String findCooldownError(
    RequestDto requestDto,
    TransferResponseDto transferResponseDto,
    Map<String, String> commandMap
  ) {
    Optional<AnglerEntity> anglerTimestamp =
      anglersService.getAnglerByGuildIdAndUserId(
        requestDto.getGuildId(),
        requestDto.getUserId()
      );

    if (anglerTimestamp.isEmpty()) {
      return null;
    }

    Integer fishingFrequency = transferResponseDto
      .getGuildConfigurations()
      .getFishingFrequency();
    Integer guildTime = (fishingFrequency != null && fishingFrequency != 0)
      ? fishingFrequency
      : DEFAULT_FISHING_FREQUENCY_MINUTES;

    Date timestamp = anglerTimestamp.get().getTimestamp();
    long remaining =
      timestamp.getTime() +
      (guildTime * 60 * 1000L) -
      System.currentTimeMillis();

    if (remaining <= 0) {
      return null;
    }

    return (
      "<@" +
      requestDto.getUserId() +
      "> must wait " +
      TimeUtil.formatTimeRemaining(remaining) +
      " to </" +
      Constants.COMMAND_NAME_FISH +
      ":" +
      commandMap.get(Constants.COMMAND_NAME_FISH) +
      "> again."
    );
  }

  /** Weighted lottery over the pool, where a creature's odds are its weight. */
  private CreatureDto selectWeightedCreature(List<CreatureDto> catchPool) {
    int totalOdds = 0;
    for (CreatureDto creature : catchPool) {
      totalOdds += creature.getOdds();
    }

    if (totalOdds <= 0) {
      return null;
    }

    int rand = new SecureRandom().nextInt(totalOdds);

    int cumulative = 0;
    for (CreatureDto creature : catchPool) {
      cumulative += creature.getOdds();
      if (rand < cumulative) {
        return creature;
      }
    }

    return null;
  }
}
