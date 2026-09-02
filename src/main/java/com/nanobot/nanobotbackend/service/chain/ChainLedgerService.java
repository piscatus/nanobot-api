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
