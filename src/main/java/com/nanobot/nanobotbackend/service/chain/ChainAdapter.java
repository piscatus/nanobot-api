package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import java.util.Map;

/**
 * Per-protocol chain integration. One implementation per network family, chosen
 * by {@link CurrencyEntity#getProtocol()}.
 *
 * <p>Implementations own everything protocol-specific: how a deposit address is
 * obtained, how deposits are detected, and how withdrawals are signed and
 * broadcast. Everything above this line - balances, gifts, rain, drops - is
 * plain ledger arithmetic and stays protocol-agnostic.
 */
public interface ChainAdapter {
  /** Value matched against {@link CurrencyEntity#getProtocol()}. */
  String protocol();

  /**
   * One pass of deposit detection and withdrawal processing for a currency.
   * Called on a short interval, so implementations must be cheap when idle.
   */
  void processActivity(
    UserDetailsEntity botUserDetails,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  );

  /**
   * The address a user should send funds to, allocating one if needed. Nano
   * derives this from the user's seed; Monero issues a subaddress from the hot
   * wallet and stores the mapping.
   */
  String resolveDepositAddress(
    UserDetailsEntity userDetails,
    CurrencyEntity currencyEntity
  );

  /**
   * Whether the network has a representative to update. Only Nano forks do, so
   * this gates the /update command.
   */
  default boolean supportsRepresentative() {
    return false;
  }

  /**
   * Whether sending costs a network fee.
   *
   * <p>Defaults to true so a new adapter is treated as fee-bearing until it says
   * otherwise: assuming feeless is the dangerous direction, because it lets a
   * withdrawal be accepted that the fee will then swallow.
   *
   * <p>Used to decide whether a missing fee estimate should block a withdrawal.
   * On a feeless network there is nothing to estimate and its absence is
   * normal.
   */
  default boolean hasNetworkFee() {
    return true;
  }

  /**
   * The fee the hot wallet would charge to send {@code raw} to {@code address}
   * right now, obtained by having the wallet build the transaction without
   * relaying it.
   *
   * <p>The stored {@code feeEstimate} assumes a transaction size; the real fee
   * depends on how many of the wallet's outputs have to be combined, which only
   * the wallet knows. Feeless networks and adapters that cannot ask return
   * {@link FeeQuote#unavailable()}, and callers fall back to the estimate.
   */
  default FeeQuote quoteWithdrawalFee(
    CurrencyEntity currencyEntity,
    String raw,
    String address
  ) {
    return FeeQuote.unavailable();
  }
}
