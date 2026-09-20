package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import com.nanobot.nanobotbackend.service.chain.BitcoinChainAdapter.Deposit;
import java.math.BigInteger;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class BitcoinChainAdapterTest {

  // ------------------------------------------------------- derivation index

  @Test
  void derivationIndexShouldReadCurrentHardenedNotation() {
    assertEquals(
      7L,
      BitcoinChainAdapter.parseDerivationIndex("m/84h/0h/0h/0/7")
    );
  }

  /** Older Bitcoin Core builds write hardened levels with an apostrophe. */
  @Test
  void derivationIndexShouldReadLegacyHardenedNotation() {
    assertEquals(
      7L,
      BitcoinChainAdapter.parseDerivationIndex("m/84'/0'/0'/0/7")
    );
  }

  @Test
  void derivationIndexShouldAcceptZero() {
    assertEquals(
      0L,
      BitcoinChainAdapter.parseDerivationIndex("m/84h/0h/0h/0/0")
    );
  }

  /**
   * A guessed index would be written into the unique key deposits are booked
   * against, so an unreadable path has to fail rather than default.
   */
  @Test
  void derivationIndexShouldRejectAnythingUnreadable() {
    assertNull(BitcoinChainAdapter.parseDerivationIndex(null));
    assertNull(BitcoinChainAdapter.parseDerivationIndex(""));
    assertNull(BitcoinChainAdapter.parseDerivationIndex("m/84h/0h/0h/0/"));
    assertNull(BitcoinChainAdapter.parseDerivationIndex("nonsense"));
  }

  // ------------------------------------------------------------- conversions

  @Test
  void amountsShouldConvertToSatoshis() {
    assertEquals(BigInteger.valueOf(91176166L), BitcoinChainAdapter.toSatoshis(
      "0.91176166"
    ));
    assertEquals(BigInteger.ONE, BitcoinChainAdapter.toSatoshis("0.00000001"));
    assertEquals(
      BigInteger.valueOf(100000000L),
      BitcoinChainAdapter.toSatoshis("1.0")
    );
  }

  /**
   * org.json hands back a Double for a JSON float. Scaling one of those by 1e8
   * with binary arithmetic is where a satoshi goes missing, so the conversion
   * has to survive being given one.
   */
  @Test
  void doubleAmountsShouldNotLoseASatoshi() {
    assertEquals(
      BigInteger.valueOf(91176166L),
      BitcoinChainAdapter.toSatoshis(Double.valueOf(0.91176166d))
    );
    assertEquals(
      BigInteger.valueOf(20999999L),
      BitcoinChainAdapter.toSatoshis(Double.valueOf(0.20999999d))
    );
  }

  @Test
  void unreadableAmountsShouldConvertToNull() {
    assertNull(BitcoinChainAdapter.toSatoshis(null));
    assertNull(BitcoinChainAdapter.toSatoshis(JSONObject.NULL));
    assertNull(BitcoinChainAdapter.toSatoshis("not a number"));
  }

  /** Bitcoin Core documents a decimal amount; 1E-8 is not that. */
  @Test
  void satoshisShouldRenderAsPlainDecimal() {
    assertEquals("0.00000001", BitcoinChainAdapter.toBitcoin(BigInteger.ONE));
    assertEquals(
      "1.00000000",
      BitcoinChainAdapter.toBitcoin(BigInteger.valueOf(100000000L))
    );
    assertEquals(
      "0.91176166",
      BitcoinChainAdapter.toBitcoin(BigInteger.valueOf(91176166L))
    );
  }

  @Test
  void conversionsShouldRoundTrip() {
    BigInteger satoshis = BigInteger.valueOf(123456789L);
    assertEquals(
      satoshis,
      BitcoinChainAdapter.toSatoshis(BitcoinChainAdapter.toBitcoin(satoshis))
    );
  }

  // ------------------------------------------------------------- grouping

  private static JSONObject entry(
    String category,
    String txid,
    String address,
    String amount,
    long confirmations
  ) throws JSONException {
    return new JSONObject()
      .put("category", category)
      .put("txid", txid)
      .put("address", address)
      .put("amount", amount)
      .put("confirmations", confirmations)
      .put("blockheight", 800000L);
  }

  /**
   * Deposit records are keyed on (ticker, txid, addressIndex), so two outputs of
   * one transaction paying the same address have to be summed. Crediting them
   * separately would book the first and let the unique index swallow the second,
   * quietly short-changing the user.
   */
  @Test
  void outputsToOneAddressInOneTransactionShouldBeSummed() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", 6))
      .put(entry("receive", "tx1", "bc1qaaa", "0.25", 6));

    Map<String, Deposit> deposits = BitcoinChainAdapter.groupDeposits(
      transactions,
      6
    );

    assertEquals(1, deposits.size());
    Deposit deposit = deposits.values().iterator().next();
    assertEquals(BigInteger.valueOf(75000000L), deposit.amount());
    assertEquals("tx1", deposit.txid());
    assertEquals("bc1qaaa", deposit.address());
  }

  @Test
  void differentAddressesInOneTransactionShouldStaySeparate() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", 6))
      .put(entry("receive", "tx1", "bc1qbbb", "0.25", 6));

    assertEquals(2, BitcoinChainAdapter.groupDeposits(transactions, 6).size());
  }

  @Test
  void immatureDepositsShouldBeSkipped() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", 5));

    assertTrue(BitcoinChainAdapter.groupDeposits(transactions, 6).isEmpty());
  }

  /**
   * The immature grouping is the mirror image: it is how a deposit is announced
   * before it can be credited, so it must pick up exactly what the credit path
   * leaves behind and nothing the credit path would take.
   */
  @Test
  void immatureGroupingShouldTakeMempoolAndShallowDepositsOnly()
    throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "mempool", "bc1qaaa", "0.1", 0))
      .put(entry("receive", "shallow", "bc1qbbb", "0.2", 5))
      .put(entry("receive", "matured", "bc1qccc", "0.3", 6));

    Map<String, Deposit> immature = BitcoinChainAdapter.groupImmatureDeposits(
      transactions,
      6
    );
    Map<String, Deposit> matured = BitcoinChainAdapter.groupDeposits(
      transactions,
      6
    );

    assertEquals(2, immature.size());
    assertTrue(immature.containsKey("mempool|bc1qaaa"));
    assertTrue(immature.containsKey("shallow|bc1qbbb"));
    assertEquals(0L, immature.get("mempool|bc1qaaa").confirmations());
    assertEquals(5L, immature.get("shallow|bc1qbbb").confirmations());

    assertEquals(1, matured.size());
    assertTrue(matured.containsKey("matured|bc1qccc"));
  }

  /**
   * A negative count is bitcoind's way of saying the transaction conflicts with
   * one that was mined. It will never confirm, so it must not be announced as a
   * deposit on its way.
   */
  @Test
  void conflictedDepositsShouldNotBeAnnounced() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", -1));

    assertTrue(
      BitcoinChainAdapter.groupImmatureDeposits(transactions, 6).isEmpty()
    );
    assertTrue(BitcoinChainAdapter.groupDeposits(transactions, 6).isEmpty());
  }

  @Test
  void immatureOutputsToOneAddressShouldBeSummedLikeMaturedOnes()
    throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", 0))
      .put(entry("receive", "tx1", "bc1qaaa", "0.25", 0));

    Map<String, Deposit> immature = BitcoinChainAdapter.groupImmatureDeposits(
      transactions,
      6
    );

    assertEquals(1, immature.size());
    assertEquals(
      BigInteger.valueOf(75000000L),
      immature.values().iterator().next().amount()
    );
  }

  /**
   * The wallet reports both sides of an internal transfer. Only the receive side
   * is a deposit; counting the send would credit the hot wallet's own spend.
   */
  @Test
  void sendsShouldNotBeCountedAsDeposits() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("send", "tx1", "bc1qaaa", "-0.5", 6))
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", 6));

    Map<String, Deposit> deposits = BitcoinChainAdapter.groupDeposits(
      transactions,
      6
    );

    assertEquals(1, deposits.size());
    assertEquals(
      BigInteger.valueOf(50000000L),
      deposits.values().iterator().next().amount()
    );
  }

  @Test
  void zeroAndNegativeAmountsShouldBeSkipped() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(entry("receive", "tx1", "bc1qaaa", "0", 6))
      .put(entry("receive", "tx2", "bc1qbbb", "-0.5", 6));

    assertTrue(BitcoinChainAdapter.groupDeposits(transactions, 6).isEmpty());
  }

  @Test
  void malformedEntriesShouldBeSkippedRatherThanThrow() throws JSONException {
    JSONArray transactions = new JSONArray()
      .put(new JSONObject().put("category", "receive"))
      .put(new JSONObject())
      .put(entry("receive", "tx1", "bc1qaaa", "0.5", 6));

    assertEquals(1, BitcoinChainAdapter.groupDeposits(transactions, 6).size());
  }
}
