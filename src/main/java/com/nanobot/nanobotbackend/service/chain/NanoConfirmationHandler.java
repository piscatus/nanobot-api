package com.nanobot.nanobotbackend.service.chain;

import org.json.JSONObject;

/**
 * What {@link NanoWebSocketService} calls once it has a confirmed block.
 *
 * <p>Exists so the websocket client stays pure transport - connect, subscribe,
 * reconnect, parse - and every decision about queues, credits and notifications
 * remains with the Nano adapter that already owns them.
 */
public interface NanoConfirmationHandler {
  /**
   * One confirmed block from the node, as the {@code message} object of a
   * {@code confirmation} notification.
   *
   * <p>Called on a worker thread, one message at a time per currency.
   * Implementations must not assume a block is new: the node repeats
   * confirmations for the same hash, so this has to be idempotent.
   */
  void handleConfirmation(String ticker, JSONObject message);

  /**
   * Asks for a full reconciliation sweep of this currency at the next
   * opportunity, because notifications may have been missed.
   *
   * <p>Called when a subscription is established, including after a reconnect.
   * The sweep itself is left to the caller's own scheduling rather than run
   * here, so node polling never happens on a websocket callback thread.
   */
  void requestReconciliation(String ticker);
}
