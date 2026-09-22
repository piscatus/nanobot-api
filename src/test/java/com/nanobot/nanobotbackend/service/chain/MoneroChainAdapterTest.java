package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.DepositAddressEntity;
import com.nanobot.nanobotbackend.entity.DepositNoticeEntity;
import com.nanobot.nanobotbackend.entity.DepositRecordEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.repository.DepositRecordsRepository;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.DepositAddressService;
import com.nanobot.nanobotbackend.service.QueuesService;
import com.nanobot.nanobotbackend.util.Constants;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;

class MoneroChainAdapterTest {

  private MoneroWalletRpcClient rpc;
  private CurrenciesService currenciesService;
  private DepositAddressService depositAddressService;
  private DepositRecordsRepository depositRecordsRepository;
  private QueuesService queuesService;
  private ChainLedgerService chainLedgerService;
  private DepositNoticeService depositNoticeService;
  private MoneroChainAdapter adapter;
  private CurrencyEntity currency;
  private QueueEntity queue;

  @BeforeEach
  void setUp() throws Exception {
    rpc = mock(MoneroWalletRpcClient.class);
    currenciesService = mock(CurrenciesService.class);
    depositAddressService = mock(DepositAddressService.class);
    depositRecordsRepository = mock(DepositRecordsRepository.class);
    queuesService = mock(QueuesService.class);
    chainLedgerService = mock(ChainLedgerService.class);
    depositNoticeService = mock(DepositNoticeService.class);
    when(
      depositNoticeService.recordFirstSighting(
        any(),
        any(),
        any(),
        any(),
        any()
      )
    )
      .thenReturn(true);
    when(
      depositNoticeService.recordConfirmSent(any(), any(), any(), any(), any())
    )
      .thenReturn(true);
    when(depositNoticeService.unconfirmed(any())).thenReturn(List.of());
    adapter = new MoneroChainAdapter(
      rpc,
      currenciesService,
      depositAddressService,
      depositRecordsRepository,
      depositNoticeService,
      queuesService
    );
    Field ledger = MoneroChainAdapter.class.getDeclaredField(
      "chainLedgerService"
    );
    ledger.setAccessible(true);
    ledger.set(adapter, chainLedgerService);

    currency = new CurrencyEntity();
    currency.setTicker("XMR");
    currency.setName("Monero");
    currency.setPrecision("12");
    currency.setProtocol(Constants.PROTOCOL_MONERO);
    currency.setProcessDeposits(false);
    currency.setProcessWithdrawals(true);
    currency.setFeePriority("1");

    queue = new QueueEntity();
    queue.setId("q1");
    queue.setUserId("u1");
    queue.setLevel(LevelDto.SEND);
    queue.setProcessed(false);
    queue.setRaw("1000000000");
    queue.setTargetAddress("4abc");
    queue.setTicker("XMR");

    when(queuesService.getQueuesByTicker("XMR")).thenReturn(List.of(queue));
    // Recovery scans and the balance snapshot answer conclusively and cheaply.
    when(rpc.wallet(eq(currency), eq("get_transfers"), any()))
      .thenReturn(new JSONObject());
    when(rpc.wallet(eq(currency), eq("get_balance"), any()))
      .thenReturn(
        new JSONObject()
          .put("balance", 5000000000L)
          .put("unlocked_balance", 0L)
          .put("blocks_to_unlock", 7)
      );
  }

  private static BigInteger raw(long value) {
    return BigInteger.valueOf(value);
  }

  private static JSONObject daemonInfo(long height) {
    try {
      return new JSONObject().put("synchronized", true).put("height", height);
    } catch (org.json.JSONException e) {
      throw new RuntimeException(e);
    }
  }

  private void daemonAt(long... heights) {
    JSONObject first = daemonInfo(heights[0]);
    JSONObject[] rest = new JSONObject[heights.length - 1];
    for (int i = 1; i < heights.length; i++) {
      rest[i - 1] = daemonInfo(heights[i]);
    }
    when(rpc.daemon(eq(currency), eq("get_info"), isNull()))
      .thenReturn(first, rest);
  }

  private void pass() {
    adapter.processActivity(new UserDetailsEntity(), currency, Map.of());
  }

  // ------------------------------------------------------------- matching

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

  @Test
  void shouldMatchADestinationThatPaidAShareOfACombinedFee() {
    assertTrue(
      MoneroChainAdapter.matchesQueuedAmount(
        raw(100000000L),
        raw(84650000L),
        raw(30700000L)
      )
    );
  }

  @Test
  void resolveDestinationAmountShouldSumEveryOutputToTheSameAddress()
    throws Exception {
    JSONObject transfer = new JSONObject()
      .put(
        "destinations",
        new org.json.JSONArray()
          .put(
            new JSONObject()
              .put("address", "8abc")
              .put("amount", 185353333L)
          )
          .put(
            new JSONObject()
              .put("address", "8abc")
              .put("amount", 185353333L)
          )
          .put(
            new JSONObject()
              .put("address", "8abc")
              .put("amount", 135353334L)
          )
      );
    assertEquals(
      raw(506060000L),
      MoneroChainAdapter.resolveDestinationAmount(transfer, "8abc")
    );
  }

  @Test
  void splitFeeShouldGiveEqualSharesWithRemainderOnTheLast() {
    List<BigInteger> shares = MoneroChainAdapter.splitFee(raw(30700000L), 2);
    assertEquals(List.of(raw(15350000L), raw(15350000L)), shares);
    assertEquals(
      List.of(raw(10L), raw(10L), raw(11L)),
      MoneroChainAdapter.splitFee(raw(31L), 3)
    );
  }

  @Test
  void splitFeeShouldLeaveNoticesBlankWhenTheFeeIsMissing() {
    List<BigInteger> shares = MoneroChainAdapter.splitFee(null, 2);
    assertEquals(2, shares.size());
    assertNull(shares.get(0));
    assertNull(shares.get(1));
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

  // --------------------------------------------------------- classification

  /**
   * The codes are monero-wallet-rpc's. -37 is the one that only needs time;
   * -17 means the wallet genuinely does not hold enough; -16 and -46 are what
   * the fee subtraction throws when the amount cannot cover the fee.
   */
  @Test
  void refusalCodesShouldMapToTheirKinds() {
    assertEquals(
      WalletRefusal.Kind.FUNDS_LOCKED,
      MoneroChainAdapter.classifyCode(-37, "not enough unlocked money")
    );
    assertEquals(
      WalletRefusal.Kind.INSUFFICIENT_FUNDS,
      MoneroChainAdapter.classifyCode(-17, "not enough money")
    );
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      MoneroChainAdapter.classifyCode(-16, "Transaction not possible")
    );
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      MoneroChainAdapter.classifyCode(-46, "Amount must be greater than 0")
    );
    assertEquals(
      WalletRefusal.Kind.TX_TOO_LARGE,
      MoneroChainAdapter.classifyCode(-18, "Transaction would be too large")
    );
    assertEquals(
      WalletRefusal.Kind.ADDRESS_REJECTED,
      MoneroChainAdapter.classifyCode(-2, "Invalid destination address")
    );
    assertEquals(
      WalletRefusal.Kind.UNREACHABLE,
      MoneroChainAdapter.classifyCode(-38, "No connection to daemon")
    );
    assertEquals(
      WalletRefusal.Kind.OTHER,
      MoneroChainAdapter.classifyCode(-1, "unknown error")
    );
  }

  /** A wallet build that numbers things differently should still land right. */
  @Test
  void refusalMessagesShouldClassifyWhenTheCodeIsUnknown() {
    assertEquals(
      WalletRefusal.Kind.FUNDS_LOCKED,
      MoneroChainAdapter.classifyCode(null, "not enough unlocked money")
    );
    assertEquals(
      WalletRefusal.Kind.INSUFFICIENT_FUNDS,
      MoneroChainAdapter.classifyCode(null, "not enough money")
    );
    assertEquals(
      WalletRefusal.Kind.OTHER,
      MoneroChainAdapter.classifyCode(null, null)
    );
  }

  // ------------------------------------------------------------------ quote

  @Test
  void quoteShouldReturnTheWalletsFeeFromADryRun() throws Exception {
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(
        RpcResponse.success(
          new JSONObject().put("fee", 71860000L).put("weight", 3593L)
        )
      );

    FeeQuote quote = adapter.quoteWithdrawalFee(currency, "1000000000", "4abc");

    assertEquals(FeeQuote.Status.QUOTED, quote.status());
    assertEquals(raw(71860000L), quote.fee());

    ArgumentCaptor<JSONObject> params = ArgumentCaptor.forClass(
      JSONObject.class
    );
    verify(rpc).walletDetailed(eq(currency), eq("transfer"), params.capture());
    JSONObject sent = params.getValue();
    assertTrue(sent.getBoolean("do_not_relay"));
    assertEquals(0, sent.getJSONArray("subtract_fee_from_outputs").getInt(0));
    assertEquals(1, sent.getInt("priority"));
    assertEquals(
      1000000000L,
      sent.getJSONArray("destinations").getJSONObject(0).getLong("amount")
    );
  }

  /** The real send never relays a dry run's flag. */
  @Test
  void realSendParametersShouldNotCarryDoNotRelay() throws Exception {
    JSONObject params = adapter.buildTransferParams(currency, "5", "4abc");
    assertFalse(params.has("do_not_relay"));
    assertEquals(0, params.getJSONArray("subtract_fee_from_outputs").getInt(0));
  }

  /**
   * Locked funds can still be sent once they unlock, but the user has to choose
   * that wait. The quote is delayed rather than a refusal or a silent queue.
   */
  @Test
  void quoteShouldTreatLockedFundsAsDelayed() {
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    FeeQuote quote = adapter.quoteWithdrawalFee(currency, "1000000000", "4abc");
    assertEquals(FeeQuote.Status.DELAYED, quote.status());
    assertEquals(WalletRefusal.Kind.FUNDS_LOCKED, quote.refusal().kind());
  }

  @Test
  void quoteShouldTreatAnUnreachableWalletAsUnavailable() {
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.unreachable("connection refused"));

    assertEquals(
      FeeQuote.Status.UNAVAILABLE,
      adapter.quoteWithdrawalFee(currency, "1000000000", "4abc").status()
    );
  }

  @Test
  void quoteShouldRejectAFeeLargerThanTheAmountWithTheMinimum() {
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-16, "Transaction not possible"));
    when(currenciesService.formatEffectiveMinimumWithdraw(currency))
      .thenReturn("0.000060000001");

    FeeQuote quote = adapter.quoteWithdrawalFee(currency, "1000", "4abc");

    assertEquals(FeeQuote.Status.REJECTED, quote.status());
    assertEquals(
      WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
      quote.refusal().kind()
    );
    String message = quote.refusal().confirmationMessage();
    assertTrue(message.contains("Nothing has been debited"));
    assertTrue(message.contains("0.000060000001 XMR"));
  }

  @Test
  void quoteShouldTreatAZeroFeeAsUnavailable() throws Exception {
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.success(new JSONObject().put("fee", 0L)));

    assertEquals(
      FeeQuote.Status.UNAVAILABLE,
      adapter.quoteWithdrawalFee(currency, "1000000000", "4abc").status()
    );
  }

  // --------------------------------------------------------------- deferral

  /**
   * The rain scenario: the previous withdrawal's change is locked for ten
   * blocks. That is a wait, not a failure, so no attempt is counted, nothing is
   * refunded, and the user is told once that their withdrawal is queued.
   */
  @Test
  void lockedFundsShouldDeferTheSendWithoutCountingAnAttempt() {
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    pass();

    verify(queuesService).updateQueueProgress("q1", null, 100L, null, null);
    verify(queuesService, never())
      .updateQueueProgress(eq("q1"), eq(1), any(), any(), any());
    assertNull(queue.getAttempts());
    assertNull(queue.getBlockHash());
    assertFalse(queue.getProcessed());
    verify(chainLedgerService)
      .notifyWithdrawalDelayed(eq(currency), eq(queue), anyString(), any());
    verify(chainLedgerService, never())
      .refundFailedWithdrawal(any(), any(), anyString(), any());
  }

  /**
   * Locked outputs only change state when a block arrives, so asking the wallet
   * every second in between is noise. One attempt per block, one notice total.
   */
  @Test
  void deferredSendShouldRetryOnlyWhenTheChainAdvances() {
    daemonAt(100L, 100L, 101L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    pass();
    pass();
    verify(rpc, times(1)).walletDetailed(eq(currency), eq("transfer"), any());

    pass();
    verify(rpc, times(2)).walletDetailed(eq(currency), eq("transfer"), any());
    verify(chainLedgerService, times(1))
      .notifyWithdrawalDelayed(eq(currency), eq(queue), anyString(), any());
  }

  @Test
  void deferredSendShouldGoOutOnceFundsUnlock() throws Exception {
    daemonAt(100L, 101L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"))
      .thenReturn(
        RpcResponse.success(
          new JSONObject().put("tx_hash", "abc123").put("fee", 30700000L)
        )
      );

    pass();
    pass();

    assertEquals("abc123", queue.getBlockHash());
    assertTrue(queue.getProcessed());
    assertNull(queue.getAttempts());
    verify(chainLedgerService)
      .notifyWithdrawalSent(eq(currency), eq(queue), anyInt(), any(), any());
  }

  /**
   * Locked funds are a wait, not a failure, so a long stall still does not
   * refund. The entry stays queued and is retried when the next block arrives.
   */
  @Test
  void aLongWaitForLockedFundsShouldKeepDeferringNotRefund() {
    queue.setIndex(100L);
    daemonAt(280L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    pass();

    assertNull(queue.getAttempts());
    assertFalse(queue.getProcessed());
    verify(chainLedgerService, never())
      .refundFailedWithdrawal(any(), any(), anyString(), any());
    verify(queuesService, never()).deleteQueue(anyString());
  }

  private QueueEntity secondQueue() {
    QueueEntity other = new QueueEntity();
    other.setId("q2");
    other.setUserId("u2");
    other.setLevel(LevelDto.SEND);
    other.setProcessed(false);
    other.setRaw("2000000000");
    other.setTargetAddress("8xyz");
    other.setTicker("XMR");
    return other;
  }

  private static DepositAddressEntity selfSendOwner() {
    return new DepositAddressEntity("u1", "XMR", "8abc", 3L);
  }

  private static QueueEntity confirmedSelfSend(String id, String raw) {
    QueueEntity entry = new QueueEntity();
    entry.setId(id);
    entry.setUserId("u1");
    entry.setLevel(LevelDto.SEND);
    entry.setProcessed(true);
    entry.setBlockHash("combo");
    entry.setRaw(raw);
    entry.setTargetAddress("8abc");
    entry.setTicker("XMR");
    return entry;
  }

  private static JSONObject combinedSelfSendTransfer() throws Exception {
    return new JSONObject()
      .put("confirmations", 10)
      .put("height", 100)
      .put("fee", 43940000L)
      .put(
        "destinations",
        new org.json.JSONArray()
          .put(
            new JSONObject()
              .put("address", "8abc")
              .put("amount", 185353333L)
          )
          .put(
            new JSONObject()
              .put("address", "8abc")
              .put("amount", 185353333L)
          )
          .put(
            new JSONObject()
              .put("address", "8abc")
              .put("amount", 135353334L)
          )
      );
  }

  /**
   * Two withdrawals waiting on the same locked change must not take a 10-block
   * turn each. They are offered as one transfer, and when that is also locked
   * they both wait for the next block instead of the first one sending and
   * re-locking the change under the second.
   */
  @Test
  void lockedFundsShouldDeferEveryWaitingSendTogether() throws Exception {
    QueueEntity other = secondQueue();
    when(queuesService.getQueuesByTicker("XMR")).thenReturn(List.of(queue, other));
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    pass();

    ArgumentCaptor<JSONObject> params = ArgumentCaptor.forClass(
      JSONObject.class
    );
    verify(rpc, times(2)).walletDetailed(eq(currency), eq("transfer"), params.capture());
    assertEquals(2, params.getAllValues().get(0).getJSONArray("destinations").length());
    assertEquals(1, params.getAllValues().get(1).getJSONArray("destinations").length());
    verify(chainLedgerService).notifyWithdrawalDelayed(
      eq(currency),
      eq(queue),
      anyString(),
      any()
    );
    verify(chainLedgerService).notifyWithdrawalDelayed(
      eq(currency),
      eq(other),
      anyString(),
      any()
    );
    assertFalse(queue.getProcessed());
    assertFalse(other.getProcessed());
  }

  /**
   * Once the change unlocks, both waiting withdrawals go out in the same
   * transaction so neither sits behind the other's new lock.
   */
  @Test
  void waitingSendsShouldBroadcastTogetherOnceFundsUnlock() throws Exception {
    QueueEntity other = secondQueue();
    when(queuesService.getQueuesByTicker("XMR")).thenReturn(List.of(queue, other));
    daemonAt(100L, 101L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"))
      .thenReturn(
        RpcResponse.success(
          new JSONObject().put("tx_hash", "combo").put("fee", 30700000L)
        )
      );

    pass();
    pass();

    assertEquals("combo", queue.getBlockHash());
    assertEquals("combo", other.getBlockHash());
    assertTrue(queue.getProcessed());
    assertTrue(other.getProcessed());
    ArgumentCaptor<JSONObject> params = ArgumentCaptor.forClass(
      JSONObject.class
    );
    verify(rpc, times(3)).walletDetailed(eq(currency), eq("transfer"), params.capture());
    JSONObject combined = params.getAllValues().get(2);
    assertEquals(2, combined.getJSONArray("destinations").length());
    assertEquals(2, combined.getJSONArray("subtract_fee_from_outputs").length());
    verify(chainLedgerService)
      .notifyWithdrawalSent(eq(currency), eq(queue), anyInt(), any(), eq(raw(15350000L)));
    verify(chainLedgerService)
      .notifyWithdrawalSent(eq(currency), eq(other), anyInt(), any(), eq(raw(15350000L)));
  }

  /**
   * If the unlocked output covers the oldest withdrawal but not the pair, send
   * the one that fits and leave the rest waiting. Do not hold the first user
   * hostage to a later request the wallet cannot fund yet.
   */
  @Test
  void combinedSendShouldShrinkToTheOldestPrefixTheWalletCanFund()
    throws Exception {
    QueueEntity other = secondQueue();
    when(queuesService.getQueuesByTicker("XMR")).thenReturn(List.of(queue, other));
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"))
      .thenReturn(
        RpcResponse.success(
          new JSONObject().put("tx_hash", "solo").put("fee", 30700000L)
        )
      )
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    pass();

    assertEquals("solo", queue.getBlockHash());
    assertTrue(queue.getProcessed());
    assertNull(other.getBlockHash());
    assertFalse(other.getProcessed());
    verify(chainLedgerService)
      .notifyWithdrawalDelayed(eq(currency), eq(other), anyString(), any());
  }

  /**
   * A combined self-send is one deposit. That notice must follow every
   * Withdrawal Confirmed for the same transaction, not land between them.
   */
  @Test
  void combinedSelfSendShouldAnnounceDepositAfterEveryWithdrawal()
    throws Exception {
    QueueEntity first = confirmedSelfSend("q1", "200000000");
    QueueEntity second = confirmedSelfSend("q2", "200000000");
    QueueEntity third = confirmedSelfSend("q3", "150000000");
    List<QueueEntity> live = new ArrayList<>(List.of(first, second, third));
    when(queuesService.getQueuesByTicker("XMR"))
      .thenAnswer(invocation -> List.copyOf(live));
    when(queuesService.deleteQueue(anyString()))
      .thenAnswer(invocation -> {
        String id = invocation.getArgument(0);
        return live
          .stream()
          .filter(entry -> id.equals(entry.getId()))
          .findFirst()
          .map(entry -> {
            live.remove(entry);
            return entry;
          });
      });
    DepositAddressEntity owner = new DepositAddressEntity(
      "u1",
      "XMR",
      "8abc",
      3L
    );
    when(depositAddressService.getByAddress("XMR", "8abc"))
      .thenReturn(Optional.of(owner));
    DepositRecordEntity booked = new DepositRecordEntity(
      "XMR",
      "combo",
      3L,
      "u1",
      "506060000",
      "100"
    );
    booked.setTransactionId("t1");
    when(
      depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
        "XMR",
        "combo",
        3L
      )
    )
      .thenReturn(
        Optional.empty(),
        Optional.empty(),
        Optional.of(booked),
        Optional.of(booked)
      );
    when(
      chainLedgerService.creditDeposit(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        eq(false)
      )
    )
      .thenReturn("t1");
    daemonAt(100L);
    when(rpc.wallet(eq(currency), eq("get_transfer_by_txid"), any()))
      .thenReturn(
        new JSONObject().put("transfer", combinedSelfSendTransfer())
      );

    pass();

    InOrder order = inOrder(chainLedgerService);
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(first), any(), any());
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(second), any(), any());
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(third), any(), any());
    order
      .verify(chainLedgerService)
      .notifyDepositConfirmed(
        eq(currency),
        eq("u1"),
        eq("506060000"),
        eq("combo"),
        eq("8abc"),
        eq("t1"),
        any()
      );
    verify(chainLedgerService, times(1))
      .notifyDepositConfirmed(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * A racing insert used to drop the deferred notice entirely, so both
   * withdrawals confirmed and the deposit stayed silent. Reloading the row
   * keeps the confirm path alive.
   */
  @Test
  void combinedSelfSendShouldConfirmAfterARacingInsert() throws Exception {
    QueueEntity first = confirmedSelfSend("q1", "200000000");
    QueueEntity second = confirmedSelfSend("q2", "150000000");
    List<QueueEntity> live = new ArrayList<>(List.of(first, second));
    when(queuesService.getQueuesByTicker("XMR"))
      .thenAnswer(invocation -> List.copyOf(live));
    when(queuesService.deleteQueue(anyString()))
      .thenAnswer(invocation -> {
        String id = invocation.getArgument(0);
        return live
          .stream()
          .filter(entry -> id.equals(entry.getId()))
          .findFirst()
          .map(entry -> {
            live.remove(entry);
            return entry;
          });
      });
    DepositAddressEntity owner = selfSendOwner();
    when(depositAddressService.getByAddress("XMR", "8abc"))
      .thenReturn(Optional.of(owner));
    DepositRecordEntity booked = new DepositRecordEntity(
      "XMR",
      "combo",
      3L,
      "u1",
      "506060000",
      "100"
    );
    booked.setTransactionId("t1");
    when(
      depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
        "XMR",
        "combo",
        3L
      )
    )
      .thenReturn(
        Optional.empty(),
        Optional.empty(),
        Optional.of(booked),
        Optional.of(booked)
      );
    doThrow(new DuplicateKeyException("dup"))
      .when(depositRecordsRepository)
      .insert(any(DepositRecordEntity.class));
    daemonAt(100L);
    when(rpc.wallet(eq(currency), eq("get_transfer_by_txid"), any()))
      .thenReturn(
        new JSONObject().put("transfer", combinedSelfSendTransfer())
      );

    pass();

    InOrder order = inOrder(chainLedgerService);
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(first), any(), any());
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(second), any(), any());
    order
      .verify(chainLedgerService)
      .notifyDepositConfirmed(
        eq(currency),
        eq("u1"),
        eq("506060000"),
        eq("combo"),
        eq("8abc"),
        eq("t1"),
        any()
      );
    verify(chainLedgerService, never())
      .creditDeposit(any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  /**
   * Combined self-sends are discovered when broadcast, as one pending credit
   * for the sum, the same way an incoming payment is announced from the pool.
   */
  @Test
  void combinedBroadcastShouldAnnounceOneDepositDiscovered() throws Exception {
    QueueEntity other = confirmedSelfSend("q2", "150000000");
    other.setProcessed(false);
    other.setBlockHash(null);
    queue.setRaw("200000000");
    queue.setTargetAddress("8abc");
    when(queuesService.getQueuesByTicker("XMR"))
      .thenReturn(List.of(queue, other));
    when(depositAddressService.getByAddress("XMR", "8abc"))
      .thenReturn(Optional.of(selfSendOwner()));
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(
        RpcResponse.success(
          new JSONObject().put("tx_hash", "combo").put("fee", 56060000L)
        )
      );
    when(rpc.wallet(eq(currency), eq("get_transfer_by_txid"), any()))
      .thenReturn(
        new JSONObject()
          .put(
            "transfer",
            new JSONObject()
              .put("txid", "combo")
              .put("confirmations", 0)
              .put(
                "destinations",
                new JSONArray()
                  .put(
                    new JSONObject()
                      .put("address", "8abc")
                      .put("amount", 171970000L)
                  )
                  .put(
                    new JSONObject()
                      .put("address", "8abc")
                      .put("amount", 121970000L)
                  )
              )
          )
      );

    pass();

    verify(chainLedgerService)
      .notifyDepositDiscovered(
        eq(currency),
        eq("u1"),
        eq("293940000"),
        eq("combo"),
        eq("8abc"),
        eq(0L),
        eq(10),
        any()
      );
    verify(chainLedgerService, times(1))
      .notifyDepositDiscovered(
        any(),
        any(),
        any(),
        any(),
        any(),
        anyLong(),
        anyInt(),
        any()
      );
    verify(chainLedgerService, never())
      .notifyDepositConfirmed(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * The outgoing scan may book a missed credit, but it must not send deposit
   * notices. Those belong to confirmSend.
   */
  @Test
  void outgoingScanShouldCreditWithoutNotices() throws Exception {
    currency.setProcessWithdrawals(false);
    currency.setProcessDeposits(true);
    when(depositAddressService.getByAddress("XMR", "8abc"))
      .thenReturn(Optional.of(selfSendOwner()));
    when(
      chainLedgerService.creditDeposit(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        eq(false)
      )
    )
      .thenReturn("t1");
    daemonAt(100L);
    JSONObject outgoing = new JSONObject()
      .put("txid", "combo")
      .put("confirmations", 10)
      .put("height", 90)
      .put(
        "destinations",
        new JSONArray()
          .put(
            new JSONObject().put("address", "8abc").put("amount", 293940000L)
          )
      );
    when(rpc.wallet(eq(currency), eq("get_transfers"), any()))
      .thenAnswer(invocation -> {
        JSONObject params = invocation.getArgument(2);
        if (params != null && params.optBoolean("out")) {
          return new JSONObject().put("out", new JSONArray().put(outgoing));
        }
        return new JSONObject();
      });

    pass();

    verify(chainLedgerService)
      .creditDeposit(
        eq(currency),
        eq("u1"),
        eq("293940000"),
        eq("combo"),
        eq("8abc"),
        any(),
        eq(false)
      );
    verify(chainLedgerService, never())
      .notifyDepositDiscovered(
        any(),
        any(),
        any(),
        any(),
        any(),
        anyLong(),
        anyInt(),
        any()
      );
    verify(chainLedgerService, never())
      .notifyDepositConfirmed(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * A just-confirmed send unlocks change. A withdrawal that was deferred at
   * this height must be offered again in the same pass, not held for the next
   * block.
   */
  @Test
  void confirmingASendShouldRetryALockedQueueOnTheSameHeight() throws Exception {
    QueueEntity first = confirmedSelfSend("q1", "200000000");
    QueueEntity waiting = secondQueue();
    List<QueueEntity> live = new ArrayList<>(List.of(first, waiting));
    when(queuesService.getQueuesByTicker("XMR"))
      .thenAnswer(invocation -> List.copyOf(live));
    when(queuesService.deleteQueue(anyString()))
      .thenAnswer(invocation -> {
        String id = invocation.getArgument(0);
        return live
          .stream()
          .filter(entry -> id.equals(entry.getId()))
          .findFirst()
          .map(entry -> {
            live.remove(entry);
            return entry;
          });
      });
    when(depositAddressService.getByAddress("XMR", "8abc"))
      .thenReturn(Optional.of(selfSendOwner()));
    when(
      chainLedgerService.creditDeposit(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        eq(false)
      )
    )
      .thenReturn("t1");
    daemonAt(100L, 100L);
    when(rpc.wallet(eq(currency), eq("get_transfer_by_txid"), any()))
      .thenReturn(
        new JSONObject()
          .put("transfer", new JSONObject().put("confirmations", 9)),
        new JSONObject().put("transfer", combinedSelfSendTransfer()),
        new JSONObject()
      );
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"))
      .thenReturn(
        RpcResponse.success(
          new JSONObject().put("tx_hash", "follow").put("fee", 30700000L)
        )
      );

    pass();
    pass();

    assertEquals("follow", waiting.getBlockHash());
    assertTrue(waiting.getProcessed());
    verify(rpc, times(2)).walletDetailed(eq(currency), eq("transfer"), any());
    verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(first), any(), any());
    verify(chainLedgerService)
      .notifyDepositConfirmed(
        eq(currency),
        eq("u1"),
        eq("506060000"),
        eq("combo"),
        eq("8abc"),
        eq("t1"),
        any()
      );
  }

  /**
   * The last sibling used to drop the notice when destinations were missing,
   * even though the first sibling had already booked the sum. The existing
   * credit is enough.
   */
  @Test
  void lastSiblingShouldConfirmFromTheExistingCreditWithoutDestinations()
    throws Exception {
    QueueEntity first = confirmedSelfSend("q1", "200000000");
    QueueEntity second = confirmedSelfSend("q2", "150000000");
    List<QueueEntity> live = new ArrayList<>(List.of(first, second));
    when(queuesService.getQueuesByTicker("XMR"))
      .thenAnswer(invocation -> List.copyOf(live));
    when(queuesService.deleteQueue(anyString()))
      .thenAnswer(invocation -> {
        String id = invocation.getArgument(0);
        return live
          .stream()
          .filter(entry -> id.equals(entry.getId()))
          .findFirst()
          .map(entry -> {
            live.remove(entry);
            return entry;
          });
      });
    when(depositAddressService.getByAddress("XMR", "8abc"))
      .thenReturn(Optional.of(selfSendOwner()));
    DepositRecordEntity booked = new DepositRecordEntity(
      "XMR",
      "combo",
      3L,
      "u1",
      "506060000",
      "100"
    );
    booked.setTransactionId("t1");
    when(
      depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
        "XMR",
        "combo",
        3L
      )
    )
      .thenReturn(Optional.empty(), Optional.empty(), Optional.of(booked));
    when(
      chainLedgerService.creditDeposit(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        eq(false)
      )
    )
      .thenReturn("t1");
    daemonAt(100L);
    when(rpc.wallet(eq(currency), eq("get_transfer_by_txid"), any()))
      .thenReturn(
        new JSONObject().put("transfer", combinedSelfSendTransfer()),
        new JSONObject()
          .put("transfer", new JSONObject().put("confirmations", 10).put("height", 100))
      );

    pass();

    InOrder order = inOrder(chainLedgerService);
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(first), any(), any());
    order
      .verify(chainLedgerService)
      .notifyWithdrawalConfirmed(eq(currency), eq(second), any(), any());
    order
      .verify(chainLedgerService)
      .notifyDepositConfirmed(
        eq(currency),
        eq("u1"),
        eq("506060000"),
        eq("combo"),
        eq("8abc"),
        eq("t1"),
        any()
      );
    verify(chainLedgerService, times(1))
      .notifyDepositConfirmed(any(), any(), any(), any(), any(), any(), any());
  }

  /**
   * Once the queues are gone, a discovered and credited self-send still owes
   * Deposit Confirmed. The next pass sends it from the existing row.
   */
  @Test
  void owedDepositConfirmShouldBeSentAfterQueuesAreGone() {
    DepositNoticeEntity notice = new DepositNoticeEntity(
      "XMR",
      "be87",
      3L,
      "u1",
      "1743860000"
    );
    DepositRecordEntity booked = new DepositRecordEntity(
      "XMR",
      "be87",
      3L,
      "u1",
      "1743860000",
      "100"
    );
    booked.setTransactionId("t1");
    when(queuesService.getQueuesByTicker("XMR")).thenReturn(List.of());
    when(depositNoticeService.unconfirmed("XMR")).thenReturn(List.of(notice));
    when(
      depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
        "XMR",
        "be87",
        3L
      )
    )
      .thenReturn(Optional.of(booked));
    when(depositAddressService.getByAddressIndex("XMR", 3L))
      .thenReturn(Optional.of(selfSendOwner()));
    daemonAt(100L);

    pass();

    verify(chainLedgerService)
      .notifyDepositConfirmed(
        eq(currency),
        eq("u1"),
        eq("1743860000"),
        eq("be87"),
        eq("8abc"),
        eq("t1"),
        any()
      );
    verify(chainLedgerService, never())
      .notifyWithdrawalConfirmed(any(), any(), any(), any());
  }

  // --------------------------------------------------------------- refusals

  /**
   * A wallet that did not answer has not judged the send, so nothing should be
   * counted against the withdrawal. A brief outage must not cancel it.
   */
  @Test
  void anUnreachableWalletShouldNotCountAnAttempt() {
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.unreachable("connection refused"));

    pass();

    assertNull(queue.getAttempts());
    verify(queuesService, never())
      .updateQueueProgress(eq("q1"), eq(1), any(), any(), any());
    verify(chainLedgerService, never())
      .notifyWithdrawalDelayed(any(), any(), anyString(), any());
    verify(chainLedgerService, never())
      .refundFailedWithdrawal(any(), any(), anyString(), any());
  }

  @Test
  void aSuccessfulTransferWithoutATxHashShouldNotCountAnAttempt()
    throws Exception {
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.success(new JSONObject().put("fee", 30700000L)));

    pass();

    assertNull(queue.getAttempts());
    assertNull(queue.getBlockHash());
    assertFalse(queue.getProcessed());
    verify(queuesService, never())
      .updateQueueProgress(eq("q1"), eq(1), any(), any(), any());
    verify(chainLedgerService, never())
      .refundFailedWithdrawal(any(), any(), anyString(), any());
  }

  @Test
  void aBlankTxHashShouldNotMarkTheQueueProcessed() throws Exception {
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(
        RpcResponse.success(new JSONObject().put("tx_hash", "").put("fee", 1L))
      );

    pass();

    assertNull(queue.getBlockHash());
    assertFalse(queue.getProcessed());
    verify(chainLedgerService, never())
      .notifyWithdrawalSent(any(), any(), anyInt(), any(), any());
  }

  @Test
  void aGenuineRefusalShouldCountAnAttempt() {
    daemonAt(100L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-17, "not enough money"));

    pass();

    assertEquals(1, queue.getAttempts());
    verify(queuesService).updateQueueProgress("q1", 1, null, null, null);
    verify(chainLedgerService, never())
      .refundFailedWithdrawal(any(), any(), anyString(), any());
  }

  /** After the last attempt the refund names the wallet's actual reason. */
  @Test
  void exhaustedAttemptsShouldRefundWithTheRefusalsReason() {
    queue.setAttempts(2);
    queue.setIndex(100L);
    daemonAt(101L);
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-17, "not enough money"));
    when(
      chainLedgerService.refundFailedWithdrawal(
        eq(currency),
        eq(queue),
        contains("cannot cover this withdrawal"),
        any()
      )
    )
      .thenReturn("refund-1");

    pass();

    verify(chainLedgerService)
      .refundFailedWithdrawal(
        eq(currency),
        eq(queue),
        contains("returned to your balance"),
        any()
      );
    verify(queuesService).deleteQueue("q1");
  }
}
