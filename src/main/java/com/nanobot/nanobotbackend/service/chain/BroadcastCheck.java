package com.nanobot.nanobotbackend.service.chain;

/**
 * Outcome of looking for a withdrawal that may already have been broadcast.
 *
 * <p>The distinction between "definitely nothing was sent" and "could not tell"
 * is the whole point. A refund is only safe in the first case, and resending is
 * only safe in the first case. Collapsing both into a null transaction id would
 * mean a node outage could refund or resend a withdrawal that had actually gone
 * out, paying the user twice.
 *
 * <p>Shared by every adapter that can lose the result of a broadcast. How the
 * evidence is gathered differs per protocol - Bitcoin matches a wallet comment,
 * Monero matches the destination once the fee is added back, Nano looks the
 * block hash up directly - but the three outcomes are the same everywhere.
 *
 * @param conclusive whether the node answered and the result can be trusted
 * @param txid the broadcast transaction, or null if there was none
 */
public record BroadcastCheck(boolean conclusive, String txid) {
  /** The node could not be asked, so nothing may be assumed. */
  public static BroadcastCheck unknown() {
    return new BroadcastCheck(false, null);
  }

  /** Proven absent from the network; refunding or resending is safe. */
  public static BroadcastCheck notSent() {
    return new BroadcastCheck(true, null);
  }

  /** Found on the network; adopt this transaction rather than sending another. */
  public static BroadcastCheck sent(String txid) {
    return new BroadcastCheck(true, txid);
  }
}
