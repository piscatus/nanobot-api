package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.repository.DepositRecordsRepository;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.DepositAddressService;
import com.nanobot.nanobotbackend.service.QueuesService;
import com.nanobot.nanobotbackend.service.chain.BitcoinChainAdapter.Deposit;
import java.math.BigInteger;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BitcoinChainAdapterTest {

  // ----------------------------------------------------------- refusals

  /**
   * Bitcoin Core puts the real reason in the message and reuses one code per
   * RPC method, so the text is what has to be read.
   */
  @Test
  void refusalMessagesShouldMapToTheirKinds() {
    assertEquals(
      WalletRefusal.Kind.INSUFFICIENT_FUNDS,
      BitcoinChainAdapter.classifyCode(-6, "Insufficient funds")
    );
    assertEquals(
      WalletRefusal.Kind.INSUFFICIENT_FUNDS,
      BitcoinChainAdapter.classifyCode(-4, "Insufficient funds")
    );
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      BitcoinChainAdapter.classifyCode(
        -6,
        "The transaction amount is too small to pay the fee"
      )
    );
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      BitcoinChainAdapter.classifyCode(
        -4,
        "The transaction amount is too small to send after the fee has been deducted"
      )
    );
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      BitcoinChainAdapter.classifyCode(-6, "Transaction amount too small")
    );
    assertEquals(
      WalletRefusal.Kind.ADDRESS_REJECTED,
      BitcoinChainAdapter.classifyCode(-5, "Invalid address")
    );
    assertEquals(
      WalletRefusal.Kind.OTHER,
      BitcoinChainAdapter.classifyCode(-4, "Fee exceeds maximum configured by user")
    );
    assertEquals(
      WalletRefusal.Kind.OTHER,
      BitcoinChainAdapter.classifyCode(null, null)
    );
  }

  private static CurrencyEntity bitcoin() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("BTC");
    currency.setName("Bitcoin");
    currency.setPrecision("8");
    currency.setFeePriority("2");
    return currency;
  }

  private static BitcoinChainAdapter adapterWith(BitcoinRpcClient rpc) {
    CurrenciesService currenciesService = mock(CurrenciesService.class);
    when(currenciesService.getEffectiveMinimumWithdraw(any())).thenReturn("1");
    when(currenciesService.getCurrencyDecimalValue(any(), anyInt()))
      .thenReturn("0.00000001");
    return new BitcoinChainAdapter(
      rpc,
      currenciesService,
      mock(DepositAddressService.class),
      mock(DepositRecordsRepository.class),
      mock(DepositNoticeService.class),
      mock(QueuesService.class)
    );
  }

  /**
   * The quote funds the same single-output, fee-subtracted transaction the
   * real send would, at the same confirmation target, without locking any
   * coins.
   */
  @Test
  void quoteShouldFundThePsbtTheSendWouldUse() throws JSONException {
    BitcoinRpcClient rpc = mock(BitcoinRpcClient.class);
    CurrencyEntity currency = bitcoin();
    when(
      rpc.walletDetailed(
        eq(currency),
        eq("walletcreatefundedpsbt"),
        any(),
        any(),
        any(),
        any()
      )
    )
      .thenReturn(
        RpcResponse.success(
          new JSONObject()
            .put("psbt", "cHNidP8=")
            .put("fee", "0.00001234")
            .put("changepos", 1)
        )
      );

    FeeQuote quote = adapterWith(rpc).quoteWithdrawalFee(
      currency,
      "150000",
      "bc1qaaa"
    );

    assertEquals(FeeQuote.Status.QUOTED, quote.status());
    assertEquals(BigInteger.valueOf(1234L), quote.fee());

    ArgumentCaptor<Object> params = ArgumentCaptor.forClass(Object.class);
    verify(rpc).walletDetailed(
      eq(currency),
      eq("walletcreatefundedpsbt"),
      params.capture(),
      params.capture(),
      params.capture(),
      params.capture()
    );
    JSONArray outputs = (JSONArray) params.getAllValues().get(1);
    assertEquals("0.00150000", outputs.getJSONObject(0).getString("bc1qaaa"));
    JSONObject options = (JSONObject) params.getAllValues().get(3);
    assertEquals(0, options.getJSONArray("subtractFeeFromOutputs").getInt(0));
    assertEquals(6, options.getInt("conf_target"));
    assertFalse(options.getBoolean("replaceable"));
    assertFalse(options.has("lockUnspents"));
  }

  @Test
  void quoteShouldRejectAnAmountTooSmallForTheFee() {
    BitcoinRpcClient rpc = mock(BitcoinRpcClient.class);
    CurrencyEntity currency = bitcoin();
    when(
      rpc.walletDetailed(
        eq(currency),
        eq("walletcreatefundedpsbt"),
        any(),
        any(),
        any(),
        any()
      )
    )
      .thenReturn(
        RpcResponse.refused(
          -4,
          "The transaction amount is too small to send after the fee has been deducted"
        )
      );

    FeeQuote quote = adapterWith(rpc).quoteWithdrawalFee(
      currency,
      "500",
      "bc1qaaa"
    );

    assertEquals(FeeQuote.Status.REJECTED, quote.status());
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      quote.refusal().kind()
    );
  }

  @Test
  void quoteShouldTreatAnUnreachableNodeAsUnavailable() {
    BitcoinRpcClient rpc = mock(BitcoinRpcClient.class);
    CurrencyEntity currency = bitcoin();
    when(
      rpc.walletDetailed(
        eq(currency),
        eq("walletcreatefundedpsbt"),
        any(),
        any(),
        any(),
        any()
      )
    )
      .thenReturn(RpcResponse.unreachable("connection refused"));

    assertEquals(
      FeeQuote.Status.UNAVAILABLE,
      adapterWith(rpc).quoteWithdrawalFee(currency, "150000", "bc1qaaa").status()
    );
  }

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
