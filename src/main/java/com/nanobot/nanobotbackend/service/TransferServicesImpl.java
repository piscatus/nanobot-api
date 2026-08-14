package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import com.nanobot.nanobotbackend.dto.BonusesDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.UserWalletsResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferServicesImpl implements TransferServices {

  public final ActivitiesService activitiesService;

  public final CoreServices coreServices;

  public final CreaturesService creaturesService;

  public final CurrenciesService currenciesService;

  public final DropsService dropsService;

  private final TransferService transferService;

  @Autowired
  private TransferExecutorService transferExecutorService;

  public TransferServicesImpl(
    ActivitiesService activitiesService,
    CoreServices coreServices,
    CreaturesService creaturesService,
    CurrenciesService currenciesService,
    DropsService dropsService,
    TransferService transferService
  ) {
    this.activitiesService = activitiesService;
    this.coreServices = coreServices;
    this.creaturesService = creaturesService;
    this.currenciesService = currenciesService;
    this.dropsService = dropsService;
    this.transferService = transferService;
  }

  public BigInteger getMaximumEntries(
    TransferResponseDto transferResponseDto,
    Function<CurrencyDto, String> minimumGetter
  ) {
    BigInteger maximumEntries = null;

    Map<String, CurrencyDto> currencyByTicker = transferResponseDto
      .getCurrencies()
      .stream()
      .collect(
        Collectors.toMap(c -> c.getTicker().toLowerCase(), Function.identity())
      );

    Map<String, CreatureDto> creatureByName = transferResponseDto
      .getCreatures()
      .stream()
      .collect(
        Collectors.toMap(c -> c.getName().toLowerCase(), Function.identity())
      );

    for (WalletDto wallet : transferResponseDto
      .getPrimaryTransfer()
      .getWallets()) {
      CurrencyDto currency = currencyByTicker.get(
        wallet.getTicker().toLowerCase()
      );
      if (currency == null) continue; // No matching currency

      BigInteger bigRaw = new BigInteger(wallet.getRaw());
      if (maximumEntries == null || bigRaw.compareTo(maximumEntries) < 0) {
        maximumEntries = bigRaw;
      }

      String minimumStr = minimumGetter.apply(currency);
      BigInteger minimumValue = new BigInteger(minimumStr);

      String minimumHumanReadable = currenciesService.getCurrencyDecimalValue(
        minimumStr,
        Integer.parseInt(currency.getPrecision())
      );

      String rawHumanReadable = currenciesService.getCurrencyDecimalValue(
        wallet.getRaw(),
        Integer.parseInt(currency.getPrecision())
      );

      if (
        bigRaw.compareTo(BigInteger.ZERO) != 0 &&
        bigRaw.compareTo(minimumValue) < 0
      ) {
        transferResponseDto.setErrorMessage(
          String.format(
            "Transfer with ticker %s has a value of %s (%d decimal places), " +
            "smaller than the minimum of %s (%d decimal places).",
            wallet.getTicker(),
            rawHumanReadable,
            (Integer.parseInt(currency.getPrecision()) -
              wallet.getRaw().length() +
              1),
            minimumHumanReadable,
            (Integer.parseInt(currency.getPrecision()) -
              minimumStr.length() +
              1)
          )
        );
        return null;
      }
    }

    for (ItemDto item : transferResponseDto.getPrimaryTransfer().getItems()) {
      CreatureDto creature = creatureByName.get(item.getName().toLowerCase());
      if (creature == null) continue; // No matching creature

      BigInteger bigQuantity = BigInteger.valueOf(item.getQuantity());
      if (maximumEntries == null || bigQuantity.compareTo(maximumEntries) < 0) {
        maximumEntries = bigQuantity;
      }
    }

    return maximumEntries;
  }

  @Override
  public TransferResponseDto drop(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_DROP, requestDto);

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
          Constants.COMMAND_NAME_DROP,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setBonuses(new BonusesDto().getBonuses());

      transferResponseDto.setPrimaryTransfer(
        transferService.processInputs(
          Constants.COMMAND_NAME_DROP,
          transferResponseDto::setErrorMessage,
          commandMap,
          requestDto.getGuildId(),
          requestDto.getUserId(),
          requestDto.getInput(),
          !requestDto.getConfirmation()
        )
      );

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      BigInteger maximumEntries = getMaximumEntries(
        transferResponseDto,
        CurrencyDto::getMinimumDrop
      );

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      boolean randomWinnerMode =
        requestDto.getRandom() != null && requestDto.getRandom() > 0;

      if (requestDto.getUsers() != null && requestDto.getUsers() != 0) {
        if (randomWinnerMode) {
          if (
            requestDto.getUsers() > Constants.maximumDropUsers
          ) {
            transferResponseDto.setErrorMessage(
              "The number of users specified (" +
              requestDto.getUsers() +
              ") exceeds the maximum allowed (" +
              Constants.maximumDropUsers +
              ")."
            );
            return transferResponseDto;
          }
          // Keep maximumEntries as transfer-derived for random <= maximumEntries validation
        } else {
          if (
            BigInteger.valueOf(requestDto.getUsers()).compareTo(maximumEntries) >
            0
          ) {
            transferResponseDto.setErrorMessage(
              "The number of users specified (" +
              requestDto.getUsers() +
              ") exceeds the maximum entries (" +
              maximumEntries.toString() +
              ") calculated from your input: `" +
              requestDto.getInput() +
              "`"
            );
            return transferResponseDto;
          } else {
            maximumEntries = BigInteger.valueOf(requestDto.getUsers());
          }
        }
      }

      // When both users and random are specified, random winners cannot exceed join limit
      if (
        randomWinnerMode &&
        requestDto.getUsers() != null &&
        requestDto.getUsers() > 0 &&
        requestDto.getRandom() > requestDto.getUsers()
      ) {
        transferResponseDto.setErrorMessage(
          "The number of random winners (" +
          requestDto.getRandom() +
          ") cannot exceed the number of users that can join (" +
          requestDto.getUsers() +
          ")."
        );
        return transferResponseDto;
      }

      // Random winners cannot exceed the minimum divisible amount (creatures/currencies)
      if (
        randomWinnerMode &&
        BigInteger.valueOf(requestDto.getRandom()).compareTo(maximumEntries) > 0
      ) {
        transferResponseDto.setErrorMessage(
          "The random number specified (" +
          requestDto.getRandom() +
          ") exceeds the maximum entries (" +
          maximumEntries.toString() +
          ") calculated from your input: `" +
          requestDto.getInput() +
          "`"
        );
        return transferResponseDto;
      }

      // When using random winner, join limit is not restricted by creature/currency count
      String maximumEntriesForDrop;
      if (randomWinnerMode) {
        maximumEntriesForDrop =
          (requestDto.getUsers() == 0 || requestDto.getUsers() == null)
            ? String.valueOf(Constants.maximumDropUsers)
            : requestDto.getUsers().toString();
      } else {
        maximumEntriesForDrop =
          requestDto.getUsers() == 0
            ? maximumEntries.toString()
            : requestDto.getUsers().toString();
      }

      // Determine the end time
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(new Date());
      calendar.add(
        Calendar.MINUTE,
        (requestDto.getDuration() != 0)
          ? requestDto.getDuration()
          : Constants.defaultDropDuration
      );

      transferResponseDto.setDrop(
        new DropDto(
          requestDto.getChannelId(),
          requestDto.getDuration() == 0
            ? Constants.defaultDropDuration
            : requestDto.getDuration(),
          calendar.getTime(),
          requestDto.getGuildId(),
          requestDto.getInput(),
          maximumEntriesForDrop,
          null, // transfer message not yet calculated
          requestDto.getRandom(),
          requestDto.getRoleId(),
          null, // start time has not yet started yet
          null, // already set on the transfer response dto
          requestDto.getUserId()
        )
      );

      if (
        transferResponseDto.getPrimaryTransfer().getPricey() &&
        !requestDto.getConfirmation()
      ) {
        transferResponseDto.setConfirmation(true);
        return transferResponseDto;
      }

      TransferResponseDto transfer = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_DROP,
        requestDto.getGuildId(),
        requestDto.getChannelId(),
        requestDto.getUserId(),
        null,
        Collections.singletonList("0"),
        null,
        transferResponseDto
      );

      if (transfer.getCompletedPrimaryTransfers() != null) {
        DropEntity dropEntity = dropsService
          .createDrop(
            new DropDto(
              requestDto.getChannelId(),
              requestDto.getDuration() == 0
                ? Constants.defaultDropDuration
                : requestDto.getDuration(),
              calendar.getTime(),
              requestDto.getGuildId(),
              requestDto.getInput(),
              maximumEntriesForDrop,
              null, // transfer message not yet calculated
              requestDto.getRandom(),
              requestDto.getRoleId(),
              new Date(),
              transferResponseDto.getPrimaryTransfer(),
              requestDto.getUserId()
            )
          )
          .get();

        transfer.getDrop().setId(dropEntity.getId());

        activitiesService.updateOrCreateActivity(
          requestDto.getGuildId(),
          requestDto.getChannelId(),
          requestDto.getUserId()
        );
      }

      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_DROP, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }

  @Override
  public TransferResponseDto gift(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_GIFT, requestDto);

      TransferResponseDto transferResponseDto = new TransferResponseDto();

      transferResponseDto.setInput(requestDto.getInput());

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
          Constants.COMMAND_NAME_GIFT,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setBonuses(new BonusesDto().getBonuses());

      transferResponseDto.setPrimaryTransfer(
        transferService.processInputs(
          Constants.COMMAND_NAME_GIFT,
          transferResponseDto::setErrorMessage,
          commandMap,
          requestDto.getGuildId(),
          requestDto.getUserId(),
          requestDto.getInput(),
          !requestDto.getConfirmation()
        )
      );

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      getMaximumEntries(transferResponseDto, CurrencyDto::getMinimumGift);

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      if (
        transferResponseDto.getPrimaryTransfer().getPricey() &&
        !requestDto.getConfirmation()
      ) {
        transferResponseDto.setConfirmation(true);
        return transferResponseDto;
      }

      TransferResponseDto transfer = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_GIFT,
        requestDto.getGuildId(),
        requestDto.getChannelId(),
        requestDto.getUserId(),
        null,
        requestDto.getReceiverIds(),
        null,
        transferResponseDto
      );

      if (transfer.getCompletedPrimaryTransfers() != null) {
        activitiesService.updateOrCreateActivity(
          requestDto.getGuildId(),
          requestDto.getChannelId(),
          requestDto.getUserId()
        );
      }

      return transfer;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_GIFT, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }

  @Override
  public TransferResponseDto merge(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_MERGE, requestDto);

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

      String subordinateUserId = transferResponseDto
        .getUserDetails()
        .getSubordinateUserId();

      if (
        subordinateUserId == null ||
        subordinateUserId.equals("null") ||
        subordinateUserId.equals("0") ||
        subordinateUserId.isEmpty()
      ) {
        transferResponseDto.setErrorMessage(
          "You do not have a subordinate user attached to your account."
        );
        return transferResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_MERGE,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setBonuses(new BonusesDto().getBonuses());

      transferResponseDto.setPrimaryTransfer(
        transferService.processInputs(
          Constants.COMMAND_NAME_MERGE,
          transferResponseDto::setErrorMessage,
          commandMap,
          requestDto.getGuildId(),
          transferResponseDto.getUserDetails().getSubordinateUserId(),
          "all creatures + all currencies",
          !requestDto.getConfirmation()
        )
      );

      if (!requestDto.getConfirmation()) {
        transferResponseDto.setConfirmation(true);
        return transferResponseDto;
      }

      return transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_MERGE,
        requestDto.getGuildId(),
        requestDto.getChannelId(),
        subordinateUserId,
        null,
        Arrays.asList(requestDto.getUserId()),
        null,
        transferResponseDto
      );
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_MERGE, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }

  /** Most-recently-active first; null timestamps last. */
  private void sortActivitiesByLastActiveDesc(List<ActivityDto> activities) {
    if (activities == null || activities.isEmpty()) {
      return;
    }
    activities.sort(
      Comparator.comparing(
        ActivityDto::getTimestamp,
        Comparator.nullsLast(Comparator.reverseOrder())
      )
    );
  }

  /**
   * Rain passes receiver IDs in activity order into {@code executeTransfer}, but
   * {@code TransferServiceImpl#transfer} fills {@code completedTransfersMap} by iterating
   * {@code getUserWalletQuantities()} (a {@link java.util.Set} built from a {@link HashSet} of user
   * IDs), not by walking the receiver-id list. First key inserted per receiver follows that set's
   * order. The map is also a {@link HashMap}, so JSON key order is not defined regardless.
   * Re-copy into a {@link LinkedHashMap} using this sorted activity list for a stable API order.
   */
  private Map<String, TransferDto> reorderRainCompletedTransfersByActivityList(
    Map<String, TransferDto> completed,
    List<ActivityDto> activities
  ) {
    if (activities == null || activities.isEmpty()) {
      return completed;
    }
    Map<String, TransferDto> ordered = new LinkedHashMap<>();
    for (ActivityDto a : activities) {
      if (a.getUserId() == null) {
        continue;
      }
      TransferDto dto = completed.get(a.getUserId());
      if (dto != null) {
        ordered.put(a.getUserId(), dto);
      }
    }
    for (Map.Entry<String, TransferDto> e : completed.entrySet()) {
      if (!ordered.containsKey(e.getKey())) {
        ordered.put(e.getKey(), e.getValue());
      }
    }
    return ordered;
  }

  @Override
  public TransferResponseDto rain(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_RAIN, requestDto);

      TransferResponseDto transferResponseDto = new TransferResponseDto();

      transferResponseDto.setInput(requestDto.getInput());

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
          Constants.COMMAND_NAME_RAIN,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setBonuses(new BonusesDto().getBonuses());

      activitiesService.setActivities(
        requestDto,
        true,
        transferResponseDto,
        transferResponseDto::setActivities
      );

      if (
        transferResponseDto.getErrorMessage() != null ||
        transferResponseDto.getActivities().isEmpty()
      ) {
        return transferResponseDto;
      }

      sortActivitiesByLastActiveDesc(transferResponseDto.getActivities());

      transferResponseDto.setPrimaryTransfer(
        transferService.processInputs(
          Constants.COMMAND_NAME_RAIN,
          transferResponseDto::setErrorMessage,
          commandMap,
          requestDto.getGuildId(),
          requestDto.getUserId(),
          requestDto.getInput(),
          !requestDto.getConfirmation()
        )
      );

      BigInteger maximumEntries = getMaximumEntries(
        transferResponseDto,
        CurrencyDto::getMinimumRain
      );

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      if (requestDto.getUsers() != null && requestDto.getUsers() != 0) {
        if (
          BigInteger.valueOf(requestDto.getUsers()).compareTo(maximumEntries) >
          0
        ) {
          transferResponseDto.setErrorMessage(
            "The number of users specified (" +
            requestDto.getUsers() +
            ") exceeds the maximum entries (" +
            maximumEntries.toString() +
            ") calculated from your input: `" +
            requestDto.getInput() +
            "`"
          );
          return transferResponseDto;
        } else {
          maximumEntries = BigInteger.valueOf(requestDto.getUsers());
        }
      }

      if (
        requestDto.getRandom() != null &&
        requestDto.getRandom() > 0 &&
        BigInteger.valueOf(requestDto.getRandom()).compareTo(maximumEntries) > 0
      ) {
        transferResponseDto.setErrorMessage(
          "The random number specified (" +
          requestDto.getRandom() +
          ") exceeds the maximum divisibility (" +
          maximumEntries.toString() +
          ") calculated from your input."
        );
        return transferResponseDto;
      }

      int activitiesSize = transferResponseDto.getActivities().size();
      if (BigInteger.valueOf(activitiesSize).compareTo(maximumEntries) > 0) {
        transferResponseDto.setErrorMessage(
          "The number of users (" +
          activitiesSize +
          ") currently active exceeds the maximum divisibility (" +
          maximumEntries.toString() +
          ") calculated from your input."
        );
        return transferResponseDto;
      }

      Integer maxMinutes = transferResponseDto
        .getGuildConfigurations()
        .getMaximumMinutesActive();
      Integer duration = requestDto.getDuration() == 0
        ? (maxMinutes != null && maxMinutes != 0
            ? maxMinutes
            : Constants.defaultMinutesActive)
        : requestDto.getDuration();

      transferResponseDto.setDrop(
        new DropDto(
          requestDto.getChannelId(),
          duration,
          null, // end time not needed for rains
          requestDto.getGuildId(),
          requestDto.getInput(),
          requestDto.getUsers() != null && requestDto.getUsers() != 0
            ? requestDto.getUsers().toString()
            : null,
          null, // transfer message not yet calculated
          requestDto.getRandom(),
          requestDto.getRoleId(),
          null, // start time not needed for rains
          null, // already set on the transfer response dto
          requestDto.getUserId()
        )
      );

      if (
        transferResponseDto.getPrimaryTransfer().getPricey() &&
        !requestDto.getConfirmation()
      ) {
        transferResponseDto.setConfirmation(true);
        return transferResponseDto;
      }

      TransferResponseDto transfer = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_RAIN,
        requestDto.getGuildId(),
        requestDto.getChannelId(),
        requestDto.getUserId(),
        null,
        transferResponseDto
          .getActivities()
          .stream()
          .map(ActivityDto::getUserId)
          .collect(Collectors.toList()),
        null,
        transferResponseDto
      );

      // Receiver list above matches activities; completed map keys do not — see
      // reorderRainCompletedTransfersByActivityList.
      transfer
        .getCompletedPrimaryTransfers()
        .filter(map -> !map.isEmpty())
        .ifPresent(
          completed ->
            transfer.setCompletedPrimaryTransfers(
              Optional.of(
                reorderRainCompletedTransfersByActivityList(
                  completed,
                  transfer.getActivities()
                )
              )
            )
        );

      if (transfer.getCompletedPrimaryTransfers().isPresent()) {
        activitiesService.updateOrCreateActivity(
          requestDto.getGuildId(),
          requestDto.getChannelId(),
          requestDto.getUserId()
        );
      }

      return transfer;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_RAIN, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }

  @Override
  public TransferResponseDto sell(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_SELL, requestDto);

      TransferResponseDto transferResponseDto = new TransferResponseDto();

      transferResponseDto.setInput(requestDto.getInput());

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
          Constants.COMMAND_NAME_SELL,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setBonuses(new BonusesDto().getBonuses());

      transferResponseDto.setPrimaryTransfer(
        transferService.processInputs(
          Constants.COMMAND_NAME_SELL,
          transferResponseDto::setErrorMessage,
          commandMap,
          requestDto.getGuildId(),
          requestDto.getUserId(),
          requestDto.getInput(),
          !requestDto.getConfirmation()
        )
      );

      if (transferResponseDto.getPrimaryTransfer().getWallets().size() > 0) {
        transferResponseDto.setErrorMessage(
          "Invalid Input: Please specify a **number** and a **creature**."
        );
        return transferResponseDto;
      }

      Map<String, BigInteger> totalValuePerTicker = new HashMap<>();

      // Determines the sale value, should be pulled out into a util file
      for (ItemDto item : transferResponseDto.getPrimaryTransfer().getItems()) {
        for (CreatureDto creatureDto : transferResponseDto.getCreatures()) {
          if (creatureDto.getName().equalsIgnoreCase(item.getName())) {
            String ticker = creatureDto.getTicker().toUpperCase(); // Normalize

            // Convert creature value and quantity to BigDecimal
            BigDecimal value = new BigDecimal(creatureDto.getValue());
            BigDecimal quantity = new BigDecimal(item.getQuantity());

            // --- Apply Bonus Multiplier ---
            BigDecimal multiplier = BigDecimal.ONE; // default multiplier = 1.0

            for (ItemDto bonus : transferResponseDto.getBonuses()) {
              BigDecimal bonusThreshold = new BigDecimal(bonus.getQuantity());
              if (quantity.compareTo(bonusThreshold) >= 0) {
                // Use this multiplier if quantity >= threshold
                multiplier = new BigDecimal(bonus.getName()); // bonus.getName() stores multiplier value (e.g., "1.25")
              }
            }

            // Apply multiplier to value
            BigDecimal adjustedValue = value.multiply(multiplier);

            // Multiply adjusted value by quantity
            BigDecimal multiplied = adjustedValue.multiply(quantity);

            // Convert to BigInteger (truncated)
            BigInteger total = multiplied.toBigInteger();

            // Merge into map
            totalValuePerTicker.merge(ticker, total, BigInteger::add);
          }
        }
      }

      List<WalletDto> wallets = new ArrayList<>();
      for (Map.Entry<
        String,
        BigInteger
      > entry : totalValuePerTicker.entrySet()) {
        LoggingUtil.info(entry.getKey() + ": " + entry.getValue());
        wallets.add(new WalletDto(entry.getKey(), entry.getValue().toString()));
      }
      transferResponseDto.setSecondaryTransfer(
        new TransferDto(wallets, new ArrayList<>())
      );

      if (!requestDto.getConfirmation()) {
        transferResponseDto.setConfirmation(true);
        return transferResponseDto;
      }

      String systemUserId = "0";

      transferResponseDto = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_SELL,
        requestDto.getGuildId(),
        requestDto.getChannelId(),
        requestDto.getUserId(),
        systemUserId,
        Collections.singletonList(systemUserId),
        Collections.singletonList(requestDto.getUserId()),
        transferResponseDto
      );

      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_SELL, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }
}
