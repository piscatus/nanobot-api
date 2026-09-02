package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BroadcastCheckTest {

  /**
   * The distinction this type exists for. Both of these carry a null
   * transaction id, and treating them the same is what would let a node outage
   * refund or resend a withdrawal that had actually gone out.
   */
  @Test
  void unknownAndNotSentShouldNotBeInterchangeable() {
    assertNull(BroadcastCheck.unknown().txid());
    assertNull(BroadcastCheck.notSent().txid());

    assertFalse(BroadcastCheck.unknown().conclusive());
    assertTrue(BroadcastCheck.notSent().conclusive());

    assertNotEquals(BroadcastCheck.unknown(), BroadcastCheck.notSent());
  }

  @Test
  void sentShouldCarryTheTransactionAndBeConclusive() {
    BroadcastCheck check = BroadcastCheck.sent("abc123");

    assertTrue(check.conclusive());
    assertEquals("abc123", check.txid());
  }

  /**
   * Callers decide whether to act by pairing the two fields, so the only state
   * that permits a refund or a resend is conclusive with no transaction.
   */
  @Test
  void onlyNotSentShouldPermitActing() {
    assertTrue(
      BroadcastCheck.notSent().conclusive() &&
      BroadcastCheck.notSent().txid() == null
    );
    assertFalse(
      BroadcastCheck.unknown().conclusive() &&
      BroadcastCheck.unknown().txid() == null
    );
    assertFalse(
      BroadcastCheck.sent("abc").conclusive() &&
      BroadcastCheck.sent("abc").txid() == null
    );
  }
}
