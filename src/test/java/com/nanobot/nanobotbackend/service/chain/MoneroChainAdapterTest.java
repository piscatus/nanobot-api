package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class MoneroChainAdapterTest {

  private static BigInteger raw(long value) {
    return BigInteger.valueOf(value);
  }

  /**
   * Sends are built with subtract_fee_from_outputs, so the wallet reports what
   * the destination actually received. Real withdrawals from the dev wallet
   * looked like this: a queued 100000000 arrived as 69300000 alongside a
   * 30700000 fee.
   */
  @Test
  void shouldMatchADestinationReportedNetOfFee() {
    assertTrue(
      MoneroChainAdapter.matchesQueuedAmount(
        raw(100000000L),
        raw(69300000L),
        raw(30700000L)
      )
    );
  }

  /** Accepted too, so the check does not depend on the fee being subtracted. */
  @Test
  void shouldMatchADestinationReportedGross() {
    assertTrue(
      MoneroChainAdapter.matchesQueuedAmount(
        raw(100000000L),
        raw(100000000L),
        raw(30700000L)
      )
    );
  }

  /**
   * The failure this guards against: treating a real broadcast as never having
   * happened, which makes the recovery path resend and the failure path refund,
   * either of which pays the user twice.
   */
  @Test
  void shouldNotMatchAnUnrelatedTransfer() {
    assertFalse(
      MoneroChainAdapter.matchesQueuedAmount(
        raw(100000000L),
        raw(50000000L),
        raw(30700000L)
      )
    );
  }

  @Test
  void shouldNotMatchWhenTheFeeIsMissingAndTheAmountIsNet() {
    assertFalse(
      MoneroChainAdapter.matchesQueuedAmount(
        raw(100000000L),
        raw(69300000L),
        BigInteger.ZERO
      )
    );
  }

  @Test
  void shouldMatchAFeelessTransferAtItsExactAmount() {
    assertTrue(
      MoneroChainAdapter.matchesQueuedAmount(
        raw(100000000L),
        raw(100000000L),
        BigInteger.ZERO
      )
    );
  }
}
