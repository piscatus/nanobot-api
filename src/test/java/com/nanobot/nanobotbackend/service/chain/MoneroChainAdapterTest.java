package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.repository.DepositRecordsRepository;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.DepositAddressService;
import com.nanobot.nanobotbackend.service.QueuesService;
import com.nanobot.nanobotbackend.util.Constants;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MoneroChainAdapterTest {

  private MoneroWalletRpcClient rpc;
  private CurrenciesService currenciesService;
  private QueuesService queuesService;
  private ChainLedgerService chainLedgerService;
  private MoneroChainAdapter adapter;
  private CurrencyEntity currency;
  private QueueEntity queue;

  @BeforeEach
  void setUp() throws Exception {
    rpc = mock(MoneroWalletRpcClient.class);
    currenciesService = mock(CurrenciesService.class);
    queuesService = mock(QueuesService.class);
    chainLedgerService = mock(ChainLedgerService.class);
    adapter = new MoneroChainAdapter(
      rpc,
      currenciesService,
      mock(DepositAddressService.class),
      mock(DepositRecordsRepository.class),
      mock(DepositNoticeService.class),
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
   * Locked funds are the queue processor's business: it waits them out, so the
   * confirmation should go ahead with the estimate rather than refuse.
   */
  @Test
  void quoteShouldTreatLockedFundsAsUnavailable() {
    when(rpc.walletDetailed(eq(currency), eq("transfer"), any()))
      .thenReturn(RpcResponse.refused(-37, "not enough unlocked money"));

    assertEquals(
      FeeQuote.Status.UNAVAILABLE,
      adapter.quoteWithdrawalFee(currency, "1000000000", "4abc").status()
    );
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
      .notifyWithdrawalSent(eq(currency), eq(queue), anyInt(), any());
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
      .notifyWithdrawalSent(any(), any(), anyInt(), any());
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
