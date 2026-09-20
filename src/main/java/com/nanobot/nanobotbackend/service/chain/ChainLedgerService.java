package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.Map;

/**
 * Protocol-agnostic half of deposits and withdrawals: moving value on the
 * internal ledger and telling the user about it. Adapters own the chain
 * specifics and delegate here so crediting behaves identically on every network.
 */
public interface ChainLedgerService {
  /**
   * Credits a confirmed deposit to a user by minting from the system account,
   * then notifies them. Returns the transaction id, or null if the transfer
   * failed.
   */
  String creditDeposit(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    Map<String, String> commandMap
  );

  /**
   * Same as {@link #creditDeposit(CurrencyEntity, String, String, String,
   * String, Map)}, but with {@code notify} false the ledger is credited and no
   * message is written. The caller then owes a {@link #notifyDepositConfirmed}
   * once whatever it needs to say first has been said. Used when a deposit is
   * the far end of one of our own withdrawals, so the withdrawal can be
   * announced before the deposit it caused.
   */
  String creditDeposit(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    Map<String, String> commandMap,
    boolean notify
  );

  /** Tells a user their deposit has been credited. */
  void notifyDepositConfirmed(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    String transactionId,
    Map<String, String> commandMap
  );

  /**
   * Tells a user an incoming transaction to their deposit address has been
   * seen, but has not yet reached the confirmations needed to be credited.
   * Deduplication is the caller's job; this writes a message every time.
   *
   * @param confirmations how many the transaction has so far
   * @param required how many it needs before it is credited
   */
  void notifyDepositDiscovered(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    long confirmations,
    int required,
    Map<String, String> commandMap
  );

  /**
   * Tells a user their withdrawal has been broadcast and is waiting on the
   * network, so a slow chain does not look like a lost /send.
   *
   * @param required confirmations the chain needs before it is announced as
   *     confirmed
   */
  void notifyWithdrawalSent(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    int required,
    Map<String, String> commandMap
  );

  /** Notifies a user that their withdrawal reached the required confirmations. */
  void notifyWithdrawalConfirmed(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    Map<String, String> commandMap
  );

  /**
   * Credits a failed withdrawal back to the user and tells them why.
   *
   * <p>Only safe to call once the caller has confirmed nothing was broadcast,
   * since the balance was debited when the withdrawal was queued. Refunding a
   * withdrawal that did reach the network would credit the user twice.
   *
   * @param reason user-facing explanation of the failure
   */
  String refundFailedWithdrawal(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    String reason,
    Map<String, String> commandMap
  );

  /**
   * Explorer link for a transaction, from the currency's template. Returns null
   * when no template is configured, which callers render as no link.
   */
  String explorerTxUrl(CurrencyEntity currencyEntity, String txid);

  /** Explorer link for an address, from the currency's template. */
  String explorerAccountUrl(CurrencyEntity currencyEntity, String address);
}
