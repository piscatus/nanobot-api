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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FishServicesImpl implements FishServices {

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

      String requiredFishingRole = null;
      if (
        transferResponseDto.getGuildConfigurations().getFishingRole() != null
      ) {
        requiredFishingRole = transferResponseDto
          .getGuildConfigurations()
          .getFishingRole();
      }

      List<String> requiredFishingBypassRoles = null;
      if (
        transferResponseDto.getGuildConfigurations().getFishingBypassRoles() !=
        null
      ) {
        requiredFishingBypassRoles = transferResponseDto
          .getGuildConfigurations()
          .getFishingBypassRoles();
      }

      String requiredFishingRoleMessage = null;
      if (
        transferResponseDto.getGuildConfigurations().getFishingError() != null
      ) {
        requiredFishingRoleMessage = transferResponseDto
          .getGuildConfigurations()
          .getFishingError();
      }

      if (
        requiredFishingRole != null &&
        !requiredFishingRole.equals("") &&
        !requiredFishingRole.equals("0") &&
        !requestDto.getUserRoles().contains(requiredFishingRole) &&
        (requiredFishingBypassRoles == null ||
          !requestDto
            .getUserRoles()
            .stream()
            .anyMatch(requiredFishingBypassRoles::contains))
      ) {
        String errorMessage =
          "<@" +
          requestDto.getUserId() +
          "> does not currently hold the required role (<@&" +
          requiredFishingRole +
          ">) for fishing.";

        if (
          requiredFishingRoleMessage != null &&
          !requiredFishingRoleMessage.isEmpty()
        ) {
          errorMessage +=
            "\n\n**Server Message:** " + requiredFishingRoleMessage;
        }

        transferResponseDto.setErrorMessage(errorMessage);

        return transferResponseDto;
      }

      final String botUserId = System.getenv("BOT_USER_ID");

      String brokeAssBitch =
        "<@" +
        requestDto.getUserId() +
        "> cannot fish because the server balance does not meet the minimum requirement.\n" +
        "Any user can </" +
        Constants.COMMAND_NAME_GIFT +
        ":" +
        commandMap.get(Constants.COMMAND_NAME_GIFT) +
        "> <@" +
        botUserId +
        "> from within a server to contriubute to that server's fishing </" +
        Constants.COMMAND_NAME_RESERVES +
        ":" +
        commandMap.get(Constants.COMMAND_NAME_RESERVES) +
        ">!\n" +
        "See </" +
        Constants.COMMAND_NAME_HELP +
        ":" +
        commandMap.get(Constants.COMMAND_NAME_HELP) +
        "> for more information!";

      // Server Balances
      List<GuildWalletsEntity> guildWalletsList =
        guildWalletsService.getGuildsWallets(requestDto.getGuildId());
      if (guildWalletsList.isEmpty()) {
        transferResponseDto.setErrorMessage(brokeAssBitch);
        return transferResponseDto;
      }
      GuildWalletsDto guildWalletDto = new GuildWalletsDto(
        guildWalletsList.get(0)
      );
      List<WalletDto> guildWallet = guildWalletDto.getWallets();

      // Creature Values
      List<CreatureEntity> creaturesEntities = creaturesService.getCreatures();
      List<CreatureDto> creatures = new ArrayList<>();
      Map<String, BigInteger> maxCreatureValuesByTicker = new HashMap<>();

      // Step 1: Build a Map of ticker to currency enabled status
      Map<String, Boolean> currencyEnabledMap = transferResponseDto
        .getCurrencies()
        .stream()
        .collect(
          Collectors.toMap(CurrencyDto::getTicker, CurrencyDto::getEnabled)
        );

      // Step 2: Process creatures, skipping those with disabled currencies
      for (CreatureEntity creatureEntity : creaturesEntities) {
        String ticker = creatureEntity.getTicker();

        // Skip this creature if its currency is disabled or missing
        if (!Boolean.TRUE.equals(currencyEnabledMap.get(ticker))) {
          continue;
        }

        CreatureDto creature = new CreatureDto(creatureEntity);
        creatures.add(creature);

        BigInteger creatureValue = new BigInteger(creature.getValue());

        // Update the maximum creature value for this ticker
        maxCreatureValuesByTicker.merge(ticker, creatureValue, BigInteger::max);
      }

      Set<String> affordableTickers = new HashSet<>();
      Set<String> unaffordableTickers = new HashSet<>();

      for (WalletDto wallet : guildWallet) {
        String ticker = wallet.getTicker();
        BigInteger walletBalance = new BigInteger(wallet.getRaw());

        if (maxCreatureValuesByTicker.containsKey(ticker)) {
          BigInteger doubleMaxCreatureValue = maxCreatureValuesByTicker
            .get(ticker)
            .multiply(BigInteger.valueOf(2));
          if (walletBalance.compareTo(doubleMaxCreatureValue) >= 0) {
            affordableTickers.add(ticker);
          } else {
            unaffordableTickers.add(ticker);
          }
        }
      }

      if (affordableTickers.isEmpty()) {
        transferResponseDto.setErrorMessage(brokeAssBitch);
        return transferResponseDto;
      }

      creaturesEntities.removeIf(creature ->
        !affordableTickers.contains(creature.getTicker())
      );

      creatures.removeIf(creature ->
        !affordableTickers.contains(creature.getTicker())
      );

      // Declare users item set
      Set<UserItemsEntity> currentUserItemQuantities = new HashSet<>();

      // Fetch sender items
      List<UserItemsEntity> senderItemsList = userItemsService.getUsersItems(
        requestDto.getUserId()
      );

      // Initialize sender items
      if (!senderItemsList.isEmpty()) {
        currentUserItemQuantities.add(senderItemsList.get(0));
        List<ItemDto> userItems = senderItemsList.get(0).getItems();
        for (ItemDto userItem : userItems) {
          for (CreatureEntity creature : creaturesEntities) {
            if (
              userItem.getName().equalsIgnoreCase(creature.getName()) &&
              userItem.getQuantity() >= creature.getCapacity()
            ) {
              transferResponseDto.setErrorMessage(
                "<@" +
                requestDto.getUserId() +
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
              return transferResponseDto;
            }
          }
        }
      }

      // Call logCurrentItems() method before fishing
      LoggingUtil.logCurrentItems(currentUserItemQuantities);

      // Get the guilds fishing time
      Integer guildTime = (transferResponseDto
              .getGuildConfigurations()
              .getFishingFrequency() !=
            null &&
          transferResponseDto.getGuildConfigurations().getFishingFrequency() !=
          0)
        ? transferResponseDto.getGuildConfigurations().getFishingFrequency()
        : 15;

      // Get (and update) the users fish time
      Optional<AnglerEntity> anglerTimestamp =
        anglersService.getAnglerByGuildIdAndUserId(
          requestDto.getGuildId(),
          requestDto.getUserId()
        );
      if (anglerTimestamp.isPresent()) {
        AnglerEntity angler = anglerTimestamp.get();
        Date timestamp = angler.getTimestamp();
        long guildTimeInMillis = guildTime * 60 * 1000L;

        // Get the timestamp in milliseconds and add guildTime
        long timestampInMillis = timestamp.getTime();
        long updatedTimestampInMillis = timestampInMillis + guildTimeInMillis;

        // Get the current time in milliseconds
        long currentTimeInMillis = System.currentTimeMillis();

        // Calculate the remaining time (time left until currentTimeInMillis >= updatedTimestampInMillis)
        long remaining = updatedTimestampInMillis - currentTimeInMillis;

        if (remaining > 0) {
          transferResponseDto.setErrorMessage(
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

          return transferResponseDto;
        }
      }

      // Calculate total odds
      int totalOdds = 0;
      for (CreatureDto creature : creatures) {
        totalOdds += creature.getOdds();
      }

      // Pick a random number between 0 and total odds - 1
      SecureRandom secureRand = new SecureRandom();
      int rand = secureRand.nextInt(totalOdds);

      // Loop through and find the selected creature
      int cumulative = 0;
      for (CreatureDto creature : creatures) {
        cumulative += creature.getOdds();
        if (rand < cumulative) {
          transferResponseDto.setConfirmation(true);
          transferResponseDto.setPrimaryTransfer(
            new TransferDto(
              Collections.singletonList(
                new WalletDto(
                  creature.getTicker(),
                  (new BigInteger(creature.getValue()).multiply(
                      BigInteger.valueOf(2L)
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

          TransferResponseDto transfer =
            transferExecutorService.executeTransfer(
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
        }
      }

      transferResponseDto.setErrorMessage(
        "<@" + requestDto.getUserId() + "> crashed their fishing boat."
      );

      return transferResponseDto;
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
}
