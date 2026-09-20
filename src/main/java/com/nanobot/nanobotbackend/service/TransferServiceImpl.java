package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.NumberUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransferServiceImpl implements TransferService {

  private final AliasesService aliasesService;
  private final CurrenciesService currenciesService;
  private final CreaturesService creaturesService;
  private final UserItemsService userItemsService;
  private final UserWalletsService userWalletsService;

  private static final String NO_ALL_ADDITIONS_ERROR =
    "Invalid Input: Cannot add to an \"ALL\" value!";

  public TransferServiceImpl(
    AliasesService aliasesService,
    CreaturesService creaturesService,
    CurrenciesService currenciesService,
    UserItemsService userItemsService,
    UserWalletsService userWalletsService
  ) {
    this.aliasesService = aliasesService;
    this.creaturesService = creaturesService;
    this.currenciesService = currenciesService;
    this.userItemsService = userItemsService;
    this.userWalletsService = userWalletsService;
  }

  public boolean updateItemQuantity(
    Consumer<String> setErrorMessage,
    TransferDto combinedTransferAmounts,
    String itemName,
    int quantity,
    boolean isRequired
  ) {
    Optional<ItemDto> existingItemOpt = combinedTransferAmounts
      .getItems()
      .stream()
      .filter(i -> i.getName().equals(itemName))
      .findFirst();
    if (existingItemOpt.isPresent()) {
      ItemDto existingItem = existingItemOpt.get();
      if (existingItem.getQuantity() == 0 || quantity == 0) {
        setErrorMessage.accept(NO_ALL_ADDITIONS_ERROR);
        return false;
      }
      int newAmount = existingItem.getQuantity() + quantity;
      existingItem.setQuantity(newAmount);
    } else {
      combinedTransferAmounts
        .getItems()
        .add(new ItemDto(itemName, quantity, isRequired));
    }
    return true;
  }

  public boolean updateWalletAmount(
    Consumer<String> setErrorMessage,
    TransferDto combinedTransferAmounts,
    CurrencyDto currency,
    BigDecimal value,
    boolean isRequired
  ) {
    Optional<WalletDto> existingWalletOpt = combinedTransferAmounts
      .getWallets()
      .stream()
      .filter(w -> w.getTicker().equalsIgnoreCase(currency.getTicker()))
      .findFirst();
    if (existingWalletOpt.isPresent()) {
      WalletDto existingWallet = existingWalletOpt.get();
      if (
        existingWallet.getRaw().equals("0") ||
        value.compareTo(BigDecimal.ZERO) == 0
      ) {
        setErrorMessage.accept(NO_ALL_ADDITIONS_ERROR);
        return false;
      }
      BigDecimal newAmount = new BigDecimal(existingWallet.getRaw()).add(value);
      existingWallet.setRaw(newAmount.toPlainString());
    } else {
      combinedTransferAmounts
        .getWallets()
        .add(
          new WalletDto(currency.getTicker(), value.toPlainString(), isRequired)
        );
    }
    return true;
  }

  public TransferDto processInputs(
    String command,
    Consumer<String> setErrorMessage,
    Map<String, String> commandMap,
    String guildId,
    String userId,
    String inputs,
    boolean checkForPricey
  ) {
    TransferDto combinedTransferAmounts = new TransferDto();

    String[] parts = inputs.contains("+")
      ? inputs.split("\\+")
      : new String[] { inputs };

    if (parts.length > Constants.maximumTransferParts) {
      setErrorMessage.accept(
        "Invalid Input: The maximum number of concatenated transfers is " +
        Constants.maximumTransferParts
      );
      return combinedTransferAmounts;
    }

    for (String input : parts) {
      String[] tokens = input.trim().split(" ");
      String first = tokens[0];
      String second = tokens.length > 1 ? tokens[1] : null;

      AtomicReference<String> errorRef = new AtomicReference<>();

      if (
        processAlias(errorRef::set, combinedTransferAmounts, guildId, first)
      ) {
        continue;
      } else {
        if (errorRef.get() != null) {
          setErrorMessage.accept(errorRef.get());
          return combinedTransferAmounts;
        }
      }
      if (processCreature(errorRef::set, combinedTransferAmounts, first)) {
        continue;
      } else {
        if (errorRef.get() != null) {
          setErrorMessage.accept(errorRef.get());
          return combinedTransferAmounts;
        }
      }
      if (processCurrency(errorRef::set, combinedTransferAmounts, first)) {
        continue;
      } else {
        if (errorRef.get() != null) {
          setErrorMessage.accept(errorRef.get());
          return combinedTransferAmounts;
        }
      }
      if (
        processAliasWithAmount(
          errorRef::set,
          combinedTransferAmounts,
          guildId,
          first,
          second
        )
      ) {
        continue;
      } else {
        if (errorRef.get() != null) {
          setErrorMessage.accept(errorRef.get());
          return combinedTransferAmounts;
        }
      }
      if (
        processCreatureWithAmount(
          errorRef::set,
          combinedTransferAmounts,
          first,
          second,
          true
        )
      ) {
        continue;
      } else {
        if (errorRef.get() != null) {
          setErrorMessage.accept(errorRef.get());
          return combinedTransferAmounts;
        }
      }
      if (
        processCurrencyWithAmount(
          errorRef::set,
          combinedTransferAmounts,
          first,
          second,
          true
        )
      ) {
        continue;
      } else {
        if (errorRef.get() != null) {
          setErrorMessage.accept(errorRef.get());
          return combinedTransferAmounts;
        }
      }

      String intro =
        "Invalid Input: " +
        first +
        (second != null ? " " + second : "") +
        "\n\n";
      if (
        command.equals(Constants.COMMAND_NAME_CONFIG) ||
        command.equals(Constants.COMMAND_NAME_SEND)
      ) {
        setErrorMessage.accept(
          intro + "Try inputs like `0.1 XNO` or `3.14 banano`."
        );
      } else if (command.equals(Constants.COMMAND_NAME_SELL)) {
        setErrorMessage.accept(
          intro + "Try inputs like `all shrimp` or `100 turtles`."
        );
      } else {
        setErrorMessage.accept(
          intro + "Try inputs like `1 nano` or `2 turtles`."
        );
      }
      return combinedTransferAmounts;
    }

    // --- 1. Wallet Handling ---
    boolean needsWalletFetch = combinedTransferAmounts
      .getWallets()
      .stream()
      .anyMatch(wallet -> "0".equals(wallet.getRaw()));

    UserWalletsDto userWallets = null;
    if (needsWalletFetch) {
      List<UserWalletsEntity> walletsEntities =
        userWalletsService.getUsersWallets(userId);
      if (walletsEntities.isEmpty()) {
        for (WalletDto wallet : combinedTransferAmounts.getWallets()) {
          if ("0".equals(wallet.getRaw())) {
            setErrorMessage.accept(
              getWalletBalanceTransferError(
                wallet.getTicker(),
                commandMap,
                "ALL",
                wallet.getRaw()
              )
            );
            return combinedTransferAmounts;
          }
        }
      }

      // Assuming one entry per user
      UserWalletsEntity entity = walletsEntities.get(0);
      userWallets = new UserWalletsDto(entity);

      // Build lookup map from user's wallets
      Map<String, String> userWalletMap = userWallets
        .getWallets()
        .stream()
        .collect(Collectors.toMap(WalletDto::getTicker, WalletDto::getRaw));

      List<WalletDto> updatedWallets = new ArrayList<>();

      for (WalletDto wallet : combinedTransferAmounts.getWallets()) {
        if (!"0".equals(wallet.getRaw())) {
          updatedWallets.add(wallet);
        } else {
          String userRaw = userWalletMap.get(wallet.getTicker());
          if (userRaw != null && !"0".equals(userRaw)) {
            wallet.setRaw(userRaw);
            updatedWallets.add(wallet);
          } else if (wallet.getRequired()) {
            setErrorMessage.accept(
              getWalletBalanceTransferError(
                wallet.getTicker(),
                commandMap,
                "ALL",
                userRaw
              )
            );
            return combinedTransferAmounts;
          }
        }
      }

      combinedTransferAmounts.setWallets(updatedWallets);
    }

    // --- 2. Item Handling ---
    boolean needsItemFetch = combinedTransferAmounts
      .getItems()
      .stream()
      .anyMatch(item -> item.getQuantity() == 0);

    UserItemsDto userItems = null;
    if (needsItemFetch) {
      List<UserItemsEntity> itemsEntities = userItemsService.getUsersItems(
        userId
      );
      if (itemsEntities.isEmpty()) {
        for (ItemDto item : combinedTransferAmounts.getItems()) {
          if (item.getQuantity() == 0) {
            setErrorMessage.accept(
              getItemBalanceTransferError(item.getName(), commandMap, -1, 0)
            );
            return combinedTransferAmounts;
          }
        }
      }

      UserItemsEntity entity = itemsEntities.get(0);
      userItems = new UserItemsDto(entity);

      // Build lookup map from user's items
      Map<String, Integer> userItemMap = userItems
        .getItems()
        .stream()
        .collect(Collectors.toMap(ItemDto::getName, ItemDto::getQuantity));

      List<ItemDto> updatedItems = new ArrayList<>();

      for (ItemDto item : combinedTransferAmounts.getItems()) {
        if (item.getQuantity() != 0) {
          updatedItems.add(item);
        } else {
          Integer userQty = userItemMap.get(item.getName());
          if (userQty != null && userQty != 0) {
            item.setQuantity(userQty);
            updatedItems.add(item);
          } else if (item.getRequired()) {
            setErrorMessage.accept(
              getItemBalanceTransferError(item.getName(), commandMap, -1, 0)
            );
            return combinedTransferAmounts;
          }
        }
      }

      combinedTransferAmounts.setItems(updatedItems);
    }

    if (
      (combinedTransferAmounts.getWallets() == null ||
        combinedTransferAmounts.getWallets().isEmpty()) &&
      (combinedTransferAmounts.getItems() == null ||
        combinedTransferAmounts.getItems().isEmpty())
    ) {
      setErrorMessage.accept(
        "Transfer of `" +
        inputs +
        "` cannot be fulfilled by your </inventory:" +
        commandMap.get("inventory") +
        "> or </wallet:" +
        commandMap.get("wallet") +
        "> balances."
      );
      return combinedTransferAmounts;
    }

    if (checkForPricey) {
      combinedTransferAmounts.setPricey(true);
    }

    return combinedTransferAmounts;
  }

  private Optional<BigDecimal> resolveAliasAmountFromInput(
    Consumer<String> setError,
    String input,
    String expectedTicker
  ) {
    String[] tokens = input.trim().split(" ");
    if (tokens.length < 2) {
      setError.accept("Invalid alias value expression: `" + input + "`");
      return Optional.empty();
    }
    String numberStr = tokens[0];
    String currencyStr = tokens[1];
    CurrencyDto currency = currenciesService.analyzeCurrencies(currencyStr);
    if (currency == null || !currency.getEnabled()) {
      setError.accept(
        "Alias refers to unknown or disabled currency: " + currencyStr
      );
      return Optional.empty();
    }
    if (!currency.getTicker().equalsIgnoreCase(expectedTicker)) {
      setError.accept(
        "Alias currency no longer matches stored ticker " + expectedTicker
      );
      return Optional.empty();
    }
    int precision = Integer.parseInt(currency.getPrecision());
    AtomicReference<String> validated = new AtomicReference<>();
    if (
      NumberUtil.validateNumber(
        numberStr,
        currency.getName(),
        precision,
        validated::set,
        false
      )
    ) {
      BigDecimal factor = BigDecimal.TEN.pow(precision);
      BigDecimal value = new BigDecimal(validated.get())
        .multiply(factor)
        .stripTrailingZeros();
      return Optional.of(value);
    }
    if (NumberUtil.validateDollarNumber(numberStr, validated::set)) {
      if (currency.getValue() == null) {
        setError.accept(
          "Currency " + currency.getName() + " has no USD price."
        );
        return Optional.empty();
      }
      BigDecimal dollarValue = new BigDecimal(currency.getValue());
      BigDecimal factor = BigDecimal.TEN.pow(precision);
      BigDecimal value = new BigDecimal(validated.get())
        .divide(dollarValue, Constants.dollarToCryptoDecimalPlaces, RoundingMode.DOWN)
        .multiply(factor)
        .setScale(0, RoundingMode.DOWN);
      return Optional.of(value);
    }
    setError.accept(validated.get());
    return Optional.empty();
  }

  private boolean processAlias(
    Consumer<String> setError,
    TransferDto dto,
    String guildId,
    String input
  ) {
    AliasDto alias = aliasesService.analyzeAliases(guildId, input);
    if (alias != null) {
      CurrencyDto currency = currenciesService.analyzeCurrencies(
        alias.getTicker()
      );
      if (currency == null || !currency.getEnabled()) {
        setError.accept(
          alias.getSingular() +
          " has an invalid or disabled currency (" +
          alias.getTicker() +
          ")"
        );
        return false;
      }
      BigDecimal value;
      if (StringUtil.isValidString(alias.getInput())) {
        Optional<BigDecimal> resolved = resolveAliasAmountFromInput(
          setError,
          alias.getInput(),
          alias.getTicker()
        );
        if (resolved.isEmpty()) {
          return false;
        }
        value = resolved.get();
      } else if (StringUtil.isValidString(alias.getValue())) {
        value = new BigDecimal(alias.getValue());
      } else {
        setError.accept(
          alias.getSingular() + " is misconfigured (missing value and input)."
        );
        return false;
      }
      AtomicReference<String> updated = new AtomicReference<>();
      if (updateWalletAmount(updated::set, dto, currency, value, true)) {
        if (dto.getAliases() == null) {
          dto.setAliases(new ArrayList<>());
        }
        dto.getAliases().add(alias);
        return true;
      } else {
        setError.accept(updated.get());
        return false;
      }
    }
    return false;
  }

  private boolean processCreature(
    Consumer<String> setError,
    TransferDto dto,
    String input
  ) {
    CreatureDto creature = creaturesService.analyzeCreatures(input);
    if (creature != null) {
      CurrencyDto currency = currenciesService.analyzeCurrencies(
        creature.getTicker()
      );
      if (currency == null || !currency.getEnabled()) {
        setError.accept(
          creature.getName() +
          " has an invalid or disabled currency (" +
          creature.getTicker() +
          ")"
        );
        return false;
      }
      AtomicReference<String> updated = new AtomicReference<>();
      if (
        updateItemQuantity(
          updated::set,
          dto,
          creature.getName().toUpperCase(),
          1,
          true
        )
      ) {
        return true;
      } else {
        setError.accept(updated.get());
        return false;
      }
    }
    return false;
  }

  private boolean processCurrency(
    Consumer<String> setError,
    TransferDto dto,
    String input
  ) {
    CurrencyDto currency = currenciesService.analyzeCurrencies(input);
    if (currency != null) {
      if (!currency.getEnabled()) {
        setError.accept("Currency " + currency.getName() + " is disabled.");
        return false;
      }
      int precision = Integer.parseInt(currency.getPrecision());
      AtomicReference<String> updated = new AtomicReference<>();
      if (
        updateWalletAmount(
          updated::set,
          dto,
          currency,
          BigDecimal.TEN.pow(precision),
          true
        )
      ) {
        return true;
      } else {
        setError.accept(updated.get());
        return false;
      }
    }
    return false;
  }

  private boolean processAliasWithAmount(
    Consumer<String> setError,
    TransferDto dto,
    String guildId,
    String numberStr,
    String aliasStr
  ) {
    if (aliasStr == null) return false;
    AliasDto alias = aliasesService.analyzeAliases(guildId, aliasStr);
    if (alias != null) {
      CurrencyDto currency = currenciesService.analyzeCurrencies(
        alias.getTicker()
      );
      if (currency == null || !currency.getEnabled()) {
        setError.accept(
          alias.getSingular() +
          " has an invalid or disabled currency (" +
          alias.getTicker() +
          ")"
        );
        return false;
      }
      AtomicReference<String> validated = new AtomicReference<>();
      if (
        NumberUtil.validateNumber(
          numberStr,
          Constants.COMMAND_NAME_ALIASES,
          10,
          validated::set,
          false
        )
      ) {
        BigDecimal baseAmount;
        if (StringUtil.isValidString(alias.getInput())) {
          Optional<BigDecimal> resolved = resolveAliasAmountFromInput(
            setError,
            alias.getInput(),
            alias.getTicker()
          );
          if (resolved.isEmpty()) {
            return false;
          }
          baseAmount = resolved.get();
        } else if (StringUtil.isValidString(alias.getValue())) {
          baseAmount = new BigDecimal(alias.getValue());
        } else {
          setError.accept(
            alias.getSingular() + " is misconfigured (missing value and input)."
          );
          return false;
        }
        BigDecimal amount = baseAmount
          .multiply(new BigDecimal(validated.get()))
          .setScale(0, RoundingMode.DOWN);
        AtomicReference<String> updated = new AtomicReference<>();
        if (updateWalletAmount(updated::set, dto, currency, amount, true)) {
          if (dto.getAliases() == null) {
            dto.setAliases(new ArrayList<>());
          }
          dto.getAliases().add(alias);
          return true;
        } else {
          setError.accept(updated.get());
          return false;
        }
      } else {
        setError.accept(validated.get());
        return false;
      }
    }
    return false;
  }

  private boolean processCreatureWithAmount(
    Consumer<String> setError,
    TransferDto dto,
    String numberStr,
    String creatureStr,
    boolean isRequired
  ) {
    if (creatureStr == null) return false;
    if (
      (creatureStr.equalsIgnoreCase(Constants.COMMAND_NAME_CREATURES) ||
        creatureStr.equalsIgnoreCase(Constants.COMMAND_NAME_INVENTORY)) &&
      numberStr.equalsIgnoreCase("all")
    ) {
      List<CreatureEntity> creatures = creaturesService.getCreatures();
      // A creature whose currency is disabled would be rejected by
      // processCreatureWithAmount, aborting the whole "all" transfer. Skip
      // it instead so a hidden currency stays invisible to everyone else.
      Set<String> enabledTickers = enabledTickers();
      AtomicReference<String> updated = new AtomicReference<>();
      for (CreatureEntity creature : creatures) {
        if (!enabledTickers.contains(upper(creature.getTicker()))) {
          continue;
        }
        if (
          !processCreatureWithAmount(
            updated::set,
            dto,
            "all",
            creature.getName(),
            false
          )
        ) {
          setError.accept(
            "Transfer of all creatures failed:\n" + updated.get()
          );
          return false;
        }
      }
      return true;
    }
    CreatureDto creature = creaturesService.analyzeCreatures(creatureStr);
    if (creature != null) {
      CurrencyDto currency = currenciesService.analyzeCurrencies(
        creature.getTicker()
      );
      if (currency == null || !currency.getEnabled()) {
        setError.accept(
          creature.getName() +
          " has an invalid or disabled currency (" +
          creature.getTicker() +
          ")"
        );
        return false;
      }
      AtomicReference<String> validated = new AtomicReference<>();
      if (
        NumberUtil.validateNumber(
          numberStr,
          Constants.COMMAND_NAME_CREATURES,
          0,
          validated::set,
          false
        )
      ) {
        int amount = Integer.parseInt(validated.get());
        AtomicReference<String> updated = new AtomicReference<>();
        if (
          updateItemQuantity(
            updated::set,
            dto,
            creature.getName().toUpperCase(),
            amount,
            isRequired
          )
        ) {
          return true;
        } else {
          setError.accept(updated.get());
          return false;
        }
      } else {
        setError.accept(validated.get());
        return false;
      }
    }
    return false;
  }

  /** Upper-cased tickers of every currency that is currently enabled. */
  private Set<String> enabledTickers() {
    Set<String> tickers = new HashSet<>();
    for (CurrencyEntity currency : currenciesService.getCurrencies(null)) {
      if (currency.getEnabled() && currency.getTicker() != null) {
        tickers.add(upper(currency.getTicker()));
      }
    }
    return tickers;
  }

  private static String upper(String value) {
    return value == null ? "" : value.toUpperCase();
  }

  private boolean processCurrencyWithAmount(
    Consumer<String> setError,
    TransferDto dto,
    String numberStr,
    String currencyStr,
    boolean isRequired
  ) {
    if (currencyStr == null) return false;
    if (
      (currencyStr.equalsIgnoreCase(Constants.COMMAND_NAME_CURRENCIES) ||
        currencyStr.equalsIgnoreCase(Constants.COMMAND_NAME_WALLET)) &&
      numberStr.equalsIgnoreCase("all")
    ) {
      List<CurrencyEntity> currencies = currenciesService.getCurrencies(null);
      AtomicReference<String> updated = new AtomicReference<>();
      for (CurrencyEntity currency : currencies) {
        // Same reasoning as the "all creatures" path: a disabled currency is
        // not the user's to move, so leave it out rather than fail the lot.
        if (!currency.getEnabled()) {
          continue;
        }
        if (
          !processCurrencyWithAmount(
            updated::set,
            dto,
            "all",
            currency.getName(),
            false
          )
        ) {
          setError.accept(
            "Transfer of all currencies failed:\n" + updated.get()
          );
          return false;
        }
      }
      return true;
    }
    CurrencyDto currency = currenciesService.analyzeCurrencies(currencyStr);
    if (currency != null) {
      if (!currency.getEnabled()) {
        setError.accept("Currency " + currency.getName() + " is disabled.");
        return false;
      }
      int precision = Integer.parseInt(currency.getPrecision());
      AtomicReference<String> validated = new AtomicReference<>();
      if (
        NumberUtil.validateNumber(
          numberStr,
          currency.getName(),
          precision,
          validated::set,
          false
        )
      ) {
        BigDecimal factor = BigDecimal.TEN.pow(precision);
        BigDecimal value = new BigDecimal(validated.get())
          .multiply(factor)
          .stripTrailingZeros();
        AtomicReference<String> updated = new AtomicReference<>();
        if (
          updateWalletAmount(updated::set, dto, currency, value, isRequired)
        ) {
          return true;
        } else {
          setError.accept(updated.get());
          return false;
        }
      } else if (NumberUtil.validateDollarNumber(numberStr, validated::set)) {
        BigDecimal dollarValue = new BigDecimal(currency.getValue());
        BigDecimal factor = BigDecimal.TEN.pow(precision);
        BigDecimal value = new BigDecimal(validated.get())
          .divide(dollarValue, Constants.dollarToCryptoDecimalPlaces, RoundingMode.DOWN)
          .multiply(factor)
          .setScale(0, RoundingMode.DOWN);
        AtomicReference<String> updated = new AtomicReference<>();
        if (updateWalletAmount(updated::set, dto, currency, value, true)) {
          return true;
        } else {
          setError.accept(updated.get());
          return false;
        }
      } else {
        setError.accept(validated.get());
        return false;
      }
    }
    return false;
  }

  private void logTransferRequest(
    TransferDto transfer,
    List<String> receiverIds,
    String guildId,
    String userId
  ) {
    LoggingUtil.info("---TransfersService : REQUEST (transfer)");
    LoggingUtil.info("  --guildId : " + guildId);
    LoggingUtil.info("  --userId  : " + userId);
    for (WalletDto wallet : transfer.getWallets()) {
      LoggingUtil.info(wallet.toJson(true));
    }
    for (ItemDto item : transfer.getItems()) {
      LoggingUtil.info(item.toJson(true));
    }
    for (String receiver : receiverIds) {
      LoggingUtil.info("  --receiver : " + receiver);
    }
  }

  private String getIndivisibleError(String asset) {
    return "Transfer of " + asset + " is indivisible amongst receivers!";
  }

  private String getWalletBalanceTransferError(
    String walletName,
    Map<String, String> commandIdMap,
    String transferAmount,
    String senderBalance
  ) {
    CurrencyDto currency = currenciesService.analyzeCurrencies(walletName);
    return (
      "Transfer of **" +
      (transferAmount.equals("ALL")
          ? "ALL"
          : currenciesService.getCurrencyDecimalValue(
            transferAmount,
            Integer.valueOf(currency.getPrecision())
          )) +
      " " +
      walletName +
      "** " +
      currency.getEmoji() +
      " cannot be fulfilled by your </wallet:" +
      commandIdMap.get("wallet") +
      "> balance of **" +
      currenciesService.getCurrencyDecimalValue(
        senderBalance,
        Integer.valueOf(currency.getPrecision())
      ) +
      " " +
      walletName +
      "**."
    );
  }

  private String getItemBalanceTransferError(
    String itemName,
    Map<String, String> commandIdMap,
    int transferAmount,
    int senderBalance
  ) {
    return (
      "Transfer of **" +
      (transferAmount == -1 ? "ALL" : transferAmount) +
      " " +
      itemName +
      "** cannot be fulfilled by your </inventory:" +
      commandIdMap.get("inventory") +
      "> balance of **" +
      senderBalance +
      " " +
      itemName +
      "**."
    );
  }

  private GuildWalletsEntity getOrCreateGuildWalletEntity(
    TransferResponseDto transferResponseDto,
    String guildId
  ) {
    if (transferResponseDto.getGuildWalletQuantities() == null) {
      transferResponseDto.setGuildWalletQuantities(new HashSet<>());
    }
    if (transferResponseDto.getGuildWalletQuantities().isEmpty()) {
      GuildWalletsEntity newGuildWalletEntity = new GuildWalletsEntity(guildId);
      newGuildWalletEntity.setWallets(new ArrayList<>());
      transferResponseDto.getGuildWalletQuantities().add(newGuildWalletEntity);
      return newGuildWalletEntity;
    }
    return transferResponseDto.getGuildWalletQuantities().iterator().next();
  }

  private void creditGuildWallet(
    TransferResponseDto transferResponseDto,
    String guildId,
    String ticker,
    BigInteger amount
  ) {
    if (amount.compareTo(BigInteger.ZERO) <= 0) {
      return;
    }
    GuildWalletsEntity guildWalletEntity = getOrCreateGuildWalletEntity(
      transferResponseDto,
      guildId
    );
    WalletDto wallet = guildWalletEntity
      .getWallets()
      .stream()
      .filter(w -> w.getTicker().equals(ticker))
      .findFirst()
      .orElse(null);
    if (wallet == null) {
      guildWalletEntity.getWallets().add(new WalletDto(ticker, amount.toString()));
    } else {
      wallet.setRaw(new BigInteger(wallet.getRaw()).add(amount).toString());
    }
  }

  private BigInteger debitGuildWallet(
    Consumer<String> setErrorMessage,
    TransferResponseDto transferResponseDto,
    String walletName,
    BigInteger transferQuantity,
    boolean transferAll,
    BigInteger receiversCount,
    Map<String, String> commandIdMap
  ) {
    if (
      transferResponseDto.getGuildWalletQuantities() == null ||
      transferResponseDto.getGuildWalletQuantities().isEmpty()
    ) {
      return null;
    }
    GuildWalletsEntity guildWalletEntity = transferResponseDto
      .getGuildWalletQuantities()
      .iterator()
      .next();
    WalletDto guildWalletDto = guildWalletEntity
      .getWallets()
      .stream()
      .filter(w -> w.getTicker().equals(walletName))
      .findFirst()
      .orElse(null);
    if (guildWalletDto == null) {
      return null;
    }

    BigInteger senderQuantity = new BigInteger(guildWalletDto.getRaw());
    BigInteger totalTransferQuantity = transferAll
      ? senderQuantity
      : transferQuantity;
    totalTransferQuantity = totalTransferQuantity.subtract(
      totalTransferQuantity.remainder(receiversCount)
    );

    if (senderQuantity.compareTo(totalTransferQuantity) < 0) {
      setErrorMessage.accept(
        getWalletBalanceTransferError(
          walletName,
          commandIdMap,
          totalTransferQuantity.toString(),
          senderQuantity.toString()
        )
      );
      return null;
    }

    guildWalletDto.setRaw(senderQuantity.subtract(totalTransferQuantity).toString());
    return totalTransferQuantity;
  }

  public TransferResponseDto transfer(
    String guildId,
    String userId,
    List<String> receiverIds,
    TransferResponseDto transferResponseDto,
    boolean isPrimary
  ) {
    try {
      TransferDto transfer = isPrimary
        ? transferResponseDto.getPrimaryTransfer()
        : transferResponseDto.getSecondaryTransfer();

      logTransferRequest(transfer, receiverIds, guildId, userId);

      if (transfer.getAliases() != null && !transfer.getAliases().isEmpty()) {
        transferResponseDto.setAliases(transfer.getAliases());
      }

      Map<String, String> commandIdMap = transferResponseDto
        .getCommands()
        .stream()
        .filter(
          cmd ->
            "wallet".equals(cmd.getName()) || "inventory".equals(cmd.getName())
        )
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      Set<String> uniqueReceiverIds = new HashSet<>(receiverIds);
      if (uniqueReceiverIds.isEmpty()) {
        transferResponseDto.setErrorMessage("Transfer requires at least one receiver.");
        return transferResponseDto;
      }
      BigInteger receiversCount = BigInteger.valueOf(uniqueReceiverIds.size());

      String nanobotUserId = System.getenv("BOT_USER_ID");

      // Initialize completedTransfers map to track transfer amounts for each receiver
      Map<String, TransferDto> completedTransfersMap = new HashMap<>();
      Map<String, TransferDto> completedGuildTransfersMap = new HashMap<>();
      for (WalletDto transferWallet : transfer.getWallets()) {
        BigInteger transferQuantity = new BigInteger(transferWallet.getRaw());
        BigInteger totalTransferQuantity = transferQuantity;
        String walletName = transferWallet.getTicker();

        // Deduct from sender's wallet
        boolean walletFound = false;

        // This will allow for guild wallet deductions, a.k.a. the fishing reserve
        if (userId.equals(nanobotUserId)) {
          AtomicReference<String> guildWalletError = new AtomicReference<>();
          BigInteger guildDebitAmount = debitGuildWallet(
            guildWalletError::set,
            transferResponseDto,
            walletName,
            transferQuantity,
            transferWallet.getRaw().equals("0"),
            receiversCount,
            commandIdMap
          );
          if (guildWalletError.get() != null) {
            transferResponseDto.setErrorMessage(guildWalletError.get());
            return transferResponseDto;
          }
          if (guildDebitAmount != null) {
            totalTransferQuantity = guildDebitAmount;
            walletFound = true;
          }
        } else if (userId.equals("0")) {
          totalTransferQuantity = transferQuantity.subtract(
            transferQuantity.remainder(receiversCount)
          );
          walletFound = true;
        } else {
          for (UserWalletsEntity senderWallets : transferResponseDto.getUserWalletQuantities()) {
            if (senderWallets.getUserId().equalsIgnoreCase(userId)) {
              for (WalletDto senderWallet : senderWallets.getWallets()) {
                if (senderWallet.getTicker().equals(walletName)) {
                  walletFound = true; // Wallet exists in the sender's account
                  BigInteger senderBalance = new BigInteger(
                    senderWallet.getRaw()
                  );
                  BigInteger transferAmount = transferWallet.getRaw().equals("0")
                    ? senderBalance
                    : new BigInteger(transferWallet.getRaw());
                  transferAmount = transferAmount.subtract(
                    transferAmount.remainder(receiversCount)
                  );
                  totalTransferQuantity = transferAmount;
                  //make sure the receiversCount is not greater than transferAmount

                  // Check if sender has enough balance to fulfill the transfer
                  if (senderBalance.compareTo(transferAmount) < 0) {
                    transferResponseDto.setErrorMessage(
                      getWalletBalanceTransferError(
                        walletName,
                        commandIdMap,
                        transferAmount.toString(),
                        senderBalance.toString()
                      )
                    );
                    return transferResponseDto;
                  }

                  // Deduct amount from sender's wallet
                  senderWallet.setRaw(
                    String.valueOf(senderBalance.subtract(transferAmount))
                  );
                  break;
                }
              }
            }
          }
        }

        // Check if wallet was not found in the sender's account
        if (!walletFound && transferWallet.getRequired()) {
          transferResponseDto.setErrorMessage(
            getWalletBalanceTransferError(
              walletName,
              commandIdMap,
              transferWallet.getRaw(),
              "0"
            )
          );
          return transferResponseDto;
        }

        transferQuantity = totalTransferQuantity.divide(receiversCount);

        for (UserWalletsEntity receiverWallets : transferResponseDto.getUserWalletQuantities()) {
          String receiverId = receiverWallets.getUserId();
          if (uniqueReceiverIds.contains(receiverId)) {
            // This will allow for guild wallet gifts, a.k.a. the fishing reserve
            if (receiverId.equals(nanobotUserId)) {
              creditGuildWallet(
                transferResponseDto,
                guildId,
                walletName,
                transferQuantity
              );
            }
            // This will allow for withdrawals, skip the addition to the receiver
            else if (receiverId.equals("0")) {
              // System sink/source: no receiver wallet to update.
            } else {
              // Check if the wallet can be found in receiver's wallets
              walletFound = false;
              for (WalletDto wallet : receiverWallets.getWallets()) {
                if (wallet.getTicker().equals(walletName)) {
                  // Add to receiver's wallet
                  if (
                    transferWallet.getRequired() &&
                    transferQuantity.compareTo(BigInteger.ONE) < 0
                  ) {
                    transferResponseDto.setErrorMessage(
                      getIndivisibleError(walletName)
                    );
                    return transferResponseDto;
                  } else if (transferQuantity.compareTo(BigInteger.ZERO) > 0) {
                    wallet.setRaw(
                      String.valueOf(
                        new BigInteger(wallet.getRaw()).add(transferQuantity)
                      )
                    );
                    walletFound = true;
                    break;
                  }
                }
              }
              // If the wallet was not found in receiver's items, create a new entry
              if (!walletFound) {
                if (
                  transferWallet.getRequired() &&
                  transferQuantity.compareTo(BigInteger.ONE) < 0
                ) {
                  transferResponseDto.setErrorMessage(
                    getIndivisibleError(walletName)
                  );
                  return transferResponseDto;
                } else if (transferQuantity.compareTo(BigInteger.ZERO) > 0) {
                  receiverWallets
                    .getWallets()
                    .add(
                      new WalletDto(
                        walletName,
                        String.valueOf(transferQuantity)
                      )
                    );
                }
              }
            }

            if (transferQuantity.compareTo(BigInteger.ZERO) != 0) {
              completedTransfersMap
                .computeIfAbsent(receiverId, k -> new TransferDto())
                .getWallets()
                .add(
                  new WalletDto(walletName, String.valueOf(transferQuantity))
                );
            }

            if (
              receiverId.equals(nanobotUserId) &&
              transferQuantity.compareTo(BigInteger.ZERO) != 0
            ) {
              completedGuildTransfersMap
                .computeIfAbsent(receiverId, k -> new TransferDto())
                .getWallets()
                .add(
                  new WalletDto(walletName, String.valueOf(transferQuantity))
                );
            }
          }
        }
      }

      // Transfer items
      for (ItemDto transferItem : transfer.getItems()) {
        String itemName = transferItem.getName();
        int senderQuantity = 0;
        boolean itemFound = false;
        // Skip the deduction from the sender, allows for fishing
        if (userId.equals("0")) {
          itemFound = true;
        } else {
          for (UserItemsEntity senderItems : transferResponseDto.getUserItemQuantities()) {
            if (senderItems.getUserId().equalsIgnoreCase(userId)) {
              for (ItemDto senderItem : senderItems.getItems()) {
                if (senderItem.getName().equalsIgnoreCase(itemName)) {
                  itemFound = true; // Item exists in the sender's inventory
                  senderQuantity = senderItem.getQuantity();
                  int transferQuantity = transferItem.getQuantity();
                  transferQuantity =
                    transferQuantity -
                    (transferQuantity % receiversCount.intValue());

                  // Check if sender has enough quantity to fulfill the transfer
                  if (senderQuantity < transferQuantity) {
                    transferResponseDto.setErrorMessage(
                      getItemBalanceTransferError(
                        itemName,
                        commandIdMap,
                        transferQuantity,
                        senderQuantity
                      )
                    );
                    return transferResponseDto;
                  }

                  // Deduct quantity from sender's inventory
                  senderItem.setQuantity(senderQuantity - transferQuantity);
                  break;
                }
              }
            }
          }
        }

        // Check if item was not found in the sender's inventory
        if (!itemFound && transferItem.getRequired()) {
          transferResponseDto.setErrorMessage(
            getItemBalanceTransferError(
              itemName,
              commandIdMap,
              transferItem.getQuantity(),
              0
            )
          );
          return transferResponseDto;
        }

        // Determine the transfer quantity per receiver
        int transferQuantity = (transferItem.getQuantity() == 0)
          ? senderQuantity / uniqueReceiverIds.size()
          : transferItem.getQuantity() / uniqueReceiverIds.size();

        for (UserItemsEntity receiverItems : transferResponseDto.getUserItemQuantities()) {
          String receiverId = receiverItems.getUserId();
          if (uniqueReceiverIds.contains(receiverId)) {
            // This will allow for guild wallet automatic sale gifts, a.k.a. the fishing reserve
            if (receiverId.equals(nanobotUserId)) {
              CreatureEntity transferCreature = creaturesService
                .getCreatures()
                .stream()
                .filter(creature ->
                  creature.getName().equalsIgnoreCase(itemName)
                )
                .findFirst()
                .orElse(null);

              if (transferCreature == null) {
                transferResponseDto.setErrorMessage(
                  "Transfer item " + itemName + " has no configured creature mapping."
                );
                return transferResponseDto;
              }
              String creatureTicker = transferCreature.getTicker();
              BigInteger saleAmount = new BigInteger(
                transferCreature.getValue()
              )
                .multiply(BigInteger.valueOf(2L))
                .multiply(BigInteger.valueOf(transferQuantity));

              if (saleAmount.compareTo(BigInteger.ZERO) != 0) {
                TransferDto guildTransfer =
                  completedGuildTransfersMap.computeIfAbsent(receiverId, k ->
                    new TransferDto()
                  );

                List<WalletDto> wallets = guildTransfer.getWallets();

                // Find existing WalletDto with the same ticker
                WalletDto existingWallet = wallets
                  .stream()
                  .filter(w -> w.getTicker().equals(creatureTicker))
                  .findFirst()
                  .orElse(null);

                if (existingWallet != null) {
                  // Update the amount if it already exists
                  BigInteger currentRaw = new BigInteger(
                    existingWallet.getRaw()
                  );
                  existingWallet.setRaw(currentRaw.add(saleAmount).toString());
                } else {
                  wallets.add(
                    new WalletDto(creatureTicker, saleAmount.toString())
                  );
                }
              }

              creditGuildWallet(
                transferResponseDto,
                guildId,
                creatureTicker,
                saleAmount
              );
            }
            // This will allow for sales, skip the addition to the receiver
            else if (receiverId.equals("0")) {
              // System sink/source: no receiver inventory to update.
            } else {
              // Check if the item can be found in receiver's items
              itemFound = false;
              for (ItemDto item : receiverItems.getItems()) {
                if (item.getName().equalsIgnoreCase(itemName)) {
                  // Add to receiver's inventory
                  if (transferItem.getRequired() && transferQuantity < 1) {
                    transferResponseDto.setErrorMessage(
                      getIndivisibleError(itemName)
                    );
                    return transferResponseDto;
                  } else if (transferQuantity > 0) {
                    item.setQuantity(item.getQuantity() + transferQuantity);
                    itemFound = true;
                    break;
                  }
                }
              }
              // If the item was not found in receiver's items, create a new entry
              if (!itemFound) {
                if (transferItem.getRequired() && transferQuantity < 1) {
                  transferResponseDto.setErrorMessage(
                    getIndivisibleError(itemName)
                  );
                  return transferResponseDto;
                } else if (transferQuantity > 0) {
                  receiverItems
                    .getItems()
                    .add(new ItemDto(itemName, transferQuantity));
                }
              }
            }

            // Update completedTransfersMap with transfer amounts for each receiver
            if (transferQuantity != 0) {
              completedTransfersMap
                .computeIfAbsent(receiverId, k -> new TransferDto())
                .getItems()
                .add(new ItemDto(itemName, transferQuantity));
            }
          }
        }
      }

      if (isPrimary) {
        transferResponseDto.setCompletedPrimaryTransfers(
          Optional.of(completedTransfersMap)
        );
        transferResponseDto.setCompletedPrimaryGuildTransfers(
          Optional.of(completedGuildTransfersMap)
        );
      } else {
        transferResponseDto.setCompletedSecondaryTransfers(
          Optional.of(completedTransfersMap)
        );
        transferResponseDto.setCompletedSecondaryGuildTransfers(
          Optional.of(completedGuildTransfersMap)
        );
      }

      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging("TRANSFER", e);
      throw e;
    }
  }
}
