package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CurrenciesService {
  Optional<CurrencyEntity> createCurrency(CurrencyDto currencyDto);

  List<CurrencyEntity> getCurrencies(String ticker);

  Optional<CurrencyEntity> getCurrencyById(String id);

  Optional<CurrencyEntity> getCurrencyByTicker(String ticker);

  Optional<CurrencyEntity> updateCurrency(String id, CurrencyDto currencyDto);

  /**
   * Writes only the chain-tracking fields, using a targeted update rather than
   * replacing the document.
   *
   * <p>{@link #updateCurrency} exists for whole-document edits: it loads the
   * document and copies every field it knows about. That is fine for an
   * administrative edit, but wrong for a value written on a hot path, because a
   * field missing from its copy list is silently dropped. Chain adapters write
   * these three every second, so they get a method that touches nothing else.
   *
   * <p>Null arguments are left untouched. None of these fields are on
   * CurrencyDto, so internal scan state is never serialized to the frontend.
   */
  boolean updateChainState(
    String id,
    String liquidity,
    String feeEstimate,
    String lastScannedHeight
  );

  Optional<CurrencyEntity> deleteCurrency(String id);

  void setCurrencies(Consumer<List<CurrencyDto>> setCurrencies);

  String getCurrencyDecimalValue(String wholeNumber, int precision);

  String getCurrencyDollarValue(String decimalNumber, String dollarNumber);

  CurrencyDto analyzeCurrencies(String inputString);

  /**
   * Error message when raw is below the currency's minimum for an action, or
   * null when acceptable. Zero passes, matching the transfer commands.
   *
   * @param action human-readable label such as "Withdrawal", used in the message
   */
  String validateMinimumAmount(
    CurrencyDto currency,
    String raw,
    String minimumRaw,
    String action
  );

  /**
   * The withdrawal floor a user must actually clear: the configured
   * minimumWithdraw plus the current network fee estimate.
   *
   * <p>The fee is deducted from the amount sent, so a withdrawal at or below the
   * fee cannot be built at all, and by then the balance has already been
   * debited. On a feeless chain the fee estimate is absent and this is just
   * minimumWithdraw.
   */
  String getEffectiveMinimumWithdraw(CurrencyDto currency);
}
