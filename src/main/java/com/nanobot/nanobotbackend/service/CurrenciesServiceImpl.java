package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.repository.CurrenciesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CurrenciesServiceImpl implements CurrenciesService {

  private CurrenciesRepository currenciesRepository;

  private final MongoTemplate mongoTemplate;

  private final FileLogger fileLogger;

  public CurrenciesServiceImpl(
    CurrenciesRepository currenciesRepository,
    MongoTemplate mongoTemplate
  ) {
    this.fileLogger = new FileLogger("CurrenciesService");
    this.currenciesRepository = currenciesRepository;
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void setCurrencies(Consumer<List<CurrencyDto>> setCurrencies) {
    List<CurrencyEntity> entities = getCurrencies(null);
    List<CurrencyDto> currencies = new ArrayList<>();
    for (CurrencyEntity currencyEntity : entities) {
      CurrencyDto currency = new CurrencyDto(currencyEntity);
      currencies.add(currency);
    }
    setCurrencies.accept(currencies);
  }

  @Override
  public String getCurrencyDecimalValue(String wholeNumber, int precision) {
    BigDecimal rawValue = new BigDecimal(wholeNumber);
    BigDecimal divisor = new BigDecimal(10).pow(precision);
    BigDecimal actualValue = rawValue.divide(divisor, MathContext.DECIMAL128);
    return actualValue.stripTrailingZeros().toPlainString();
  }

  @Override
  public String getCurrencyDollarValue(
    String decimalNumber,
    String dollarNumber
  ) {
    BigDecimal decimalValue = new BigDecimal(decimalNumber);
    BigDecimal dollarValue = new BigDecimal(dollarNumber);

    BigDecimal result = decimalValue
      .multiply(dollarValue)
      .setScale(8, RoundingMode.DOWN);

    return result.toPlainString();
  }

  @Override
  public CurrencyDto analyzeCurrencies(String inputString) {
    if (!StringUtil.isValidString(inputString)) {
      return null;
    }
    List<CurrencyEntity> currenciesDB = getCurrencies(null);
    List<CurrencyDto> currencies = new ArrayList<>();
    for (CurrencyEntity entity : currenciesDB) {
      currencies.add(new CurrencyDto(entity));
    }
    for (CurrencyDto currency : currencies) {
      if (
        currency.getName().equalsIgnoreCase(inputString) ||
        currency.getTicker().equalsIgnoreCase(inputString) ||
        currency.getEmoji().equals(inputString)
      ) {
        return currency;
      }
    }
    return null;
  }

  @Override
  public String validateMinimumAmount(
    CurrencyDto currency,
    String raw,
    String minimumRaw,
    String action
  ) {
    if (
      currency == null ||
      !StringUtil.isValidString(raw) ||
      !StringUtil.isValidString(minimumRaw)
    ) {
      return null;
    }

    BigDecimal amount;
    BigDecimal minimum;
    try {
      amount = new BigDecimal(raw);
      minimum = new BigDecimal(minimumRaw);
    } catch (NumberFormatException e) {
      return null;
    }

    // Zero passes, matching how the transfer commands treat item-only transfers.
    if (amount.signum() == 0 || amount.compareTo(minimum) >= 0) {
      return null;
    }

    int precision = Integer.parseInt(currency.getPrecision());
    return String.format(
      "%s of %s %s is smaller than the minimum of %s %s.",
      action,
      getCurrencyDecimalValue(raw, precision),
      currency.getTicker(),
      getCurrencyDecimalValue(minimumRaw, precision),
      currency.getTicker()
    );
  }

  @Override
  public String getEffectiveMinimumWithdraw(CurrencyDto currency) {
    if (currency == null) {
      return null;
    }
    String minimum = currency.getMinimumWithdraw();
    if (!StringUtil.isValidString(minimum)) {
      return minimum;
    }
    String fee = currency.getFeeEstimate();
    if (!StringUtil.isValidString(fee)) {
      return minimum;
    }
    try {
      return new BigDecimal(minimum)
        .add(new BigDecimal(fee))
        .toBigIntegerExact()
        .toString();
    } catch (ArithmeticException | NumberFormatException e) {
      return minimum;
    }
  }

  @Override
  public String formatEffectiveMinimumWithdraw(CurrencyEntity currency) {
    if (currency == null) {
      return null;
    }
    return getCurrencyDecimalValue(
      getEffectiveMinimumWithdraw(new CurrencyDto(currency)),
      Integer.parseInt(currency.getPrecision())
    );
  }

  @Override
  public Optional<CurrencyEntity> createCurrency(CurrencyDto currencyDto) {
    CurrencyEntity currencyEntity = new CurrencyEntity(currencyDto);
    ObjectId id = new ObjectId();
    currencyEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating currency with ticker: " + currencyEntity.getTicker()
    );
    try {
      CurrencyEntity createdCurrency = currenciesRepository.insert(
        currencyEntity
      );
      fileLogger.info("Currency created with ID: " + createdCurrency.getId());
      return Optional.of(createdCurrency);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Currency already exists with specified ticker: " +
        currencyDto.getTicker()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating currency: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<CurrencyEntity> getCurrencies(String ticker) {
    try {
      if (ticker == null) {
        // fileLogger.info("Fetching all currencies.");
        return currenciesRepository.findAll();
      }
      fileLogger.info("Fetching currencies with ticker: " + ticker);
      return currenciesRepository.findByTicker(ticker);
    } catch (Exception e) {
      fileLogger.error("Error fetching currencies: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<CurrencyEntity> getCurrencyById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching currency with ID: " + id);
        return currenciesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching currency by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CurrencyEntity> getCurrencyByTicker(String ticker) {
    if (ticker != null) {
      try {
        fileLogger.info("Fetching currency with ticker: " + ticker);
        List<CurrencyEntity> existingCurrency =
          currenciesRepository.findByTicker(ticker);
        if (existingCurrency.size() > 1) {
          fileLogger.error(
            "Multiple currencies found with the same ticker: " + ticker
          );
        } else if (!existingCurrency.isEmpty()) {
          return Optional.of(existingCurrency.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching currency by ticker: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CurrencyEntity> updateCurrency(
    String id,
    CurrencyDto currencyDto
  ) {
    if (id != null) {
      try {
        Optional<CurrencyEntity> currencyOptional =
          currenciesRepository.findById(id);
        if (currencyOptional.isPresent()) {
          CurrencyEntity currency = currencyOptional.get();
          currency.setTicker(currencyDto.getTicker());
          currency.setName(currencyDto.getName());
          currency.setAddress(currencyDto.getAddress());
          currency.setNodeUrl(currencyDto.getNodeUrl());
          currency.setWebsocketUrl(currencyDto.getWebsocketUrl());
          currency.setEnabled(currencyDto.getEnabled());
          currency.setProcessDeposits(currencyDto.getProcessDeposits());
          currency.setProcessWithdrawals(currencyDto.getProcessWithdrawals());
          currency.setOpenDifficulty(currencyDto.getOpenDifficulty());
          currency.setReceiveDifficulty(currencyDto.getReceiveDifficulty());
          currency.setSendDifficulty(currencyDto.getSendDifficulty());
          currency.setUpdateDifficulty(currencyDto.getUpdateDifficulty());
          currency.setEmoji(currencyDto.getEmoji());
          currency.setColor(currencyDto.getColor());
          currency.setPrecision(currencyDto.getPrecision());
          currency.setLiquidity(currencyDto.getLiquidity());
          currency.setValue(currencyDto.getValue());
          currency.setMinimumDeposit(currencyDto.getMinimumDeposit());
          currency.setMinimumWithdraw(currencyDto.getMinimumWithdraw());
          currency.setMinimumDrop(currencyDto.getMinimumDrop());
          currency.setMinimumGift(currencyDto.getMinimumGift());
          currency.setMinimumRain(currencyDto.getMinimumRain());
          currency.setProtocol(currencyDto.getProtocol());
          currency.setConfirmations(currencyDto.getConfirmations());
          currency.setExplorerAccountUrl(currencyDto.getExplorerAccountUrl());
          currency.setExplorerTxUrl(currencyDto.getExplorerTxUrl());
          currency.setAddressFormat(currencyDto.getAddressFormat());
          currency.setFeeEstimate(currencyDto.getFeeEstimate());
          currency.setFeePriority(currencyDto.getFeePriority());
          currency.setConcealBalances(currencyDto.getConcealBalances());
          currency.setSupportsRepresentative(
            currencyDto.getSupportsRepresentative()
          );
          // walletRpcUrl, walletRpcUser, walletRpcPassword, priceId and
          // lastScannedHeight are deliberately absent from CurrencyDto and so
          // are never touched here. The credentials must not reach the Discord
          // frontend, and the scan cursor is written via
          // updateLastScannedHeight instead.

          CurrencyEntity updatedCurrency = currenciesRepository.save(currency);
          fileLogger.info("Currency updated with ID: " + id);
          return Optional.of(updatedCurrency);
        } else {
          fileLogger.warn("Currency not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating currency: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean updateChainState(
    String id,
    String liquidity,
    String feeEstimate,
    String lastScannedHeight
  ) {
    if (id == null) {
      return false;
    }
    Update update = new Update();
    if (liquidity != null) {
      update.set("liquidity", liquidity);
    }
    if (feeEstimate != null) {
      update.set("feeEstimate", feeEstimate);
    }
    if (lastScannedHeight != null) {
      update.set("lastScannedHeight", lastScannedHeight);
    }
    if (!update.getUpdateObject().containsKey("$set")) {
      return false;
    }
    try {
      mongoTemplate.updateFirst(
        Query.query(Criteria.where("_id").is(id)),
        update,
        CurrencyEntity.class
      );
      return true;
    } catch (Exception e) {
      fileLogger.error("Error updating chain state: " + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<CurrencyEntity> deleteCurrency(String id) {
    if (id != null) {
      Optional<CurrencyEntity> currencyOptional = currenciesRepository.findById(
        id
      );
      if (currencyOptional.isPresent()) {
        CurrencyEntity currencyEntity = currencyOptional.get();
        currenciesRepository.deleteById(id);
        fileLogger.info("Currency deleted with ID: " + id);
        return Optional.of(currencyEntity);
      } else {
        fileLogger.warn("Currency not found with ID: " + id);
      }
    }
    return Optional.empty();
  }
}
