package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
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
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Monero integration.
 *
 * <p>Deposits cost nothing here, unlike the Nano adapter. Nano gives every user
 * their own account, so deposits have to be swept into the hot wallet; on a
 * network with fees that sweep would cost money per deposit. Monero instead
 * issues each user a subaddress of one hot wallet, so incoming funds are already
 * in the wallet and there is no consolidation transaction at all. The only fee
 * is on withdrawal.
 *
 * <p>Deposit detection is driven off the daemon's block height rather than
 * polling the wallet every second: the wallet scan only runs when the chain has
 * actually advanced, which is roughly every two minutes. A lighter look at the
 * transaction pool runs more often so an incoming deposit can be announced
 * before it is mined. Nothing is credited until it is ten blocks deep. A send
 * to one of our own subaddresses is never reported as incoming; it is
 * discovered at broadcast and confirmed in the same step as the withdrawal.
 */
@Service
public class MoneroChainAdapter implements ChainAdapter {

  /**
   * Monero locks incoming outputs for 10 blocks. Crediting earlier would let a
   * user withdraw funds the hot wallet cannot yet spend.
   */
  private static final int DEFAULT_CONFIRMATIONS = 10;

  /**
   * Re-scan this many blocks below the cursor each pass so a small reorg cannot
   * drop a deposit. Re-scanning is safe because depositRecords has a unique
   * index that rejects a second credit for the same output.
   */
  private static final long REORG_LOOKBACK = 20L;

  /**
   * Transaction weight in bytes used to turn the daemon's per-byte fee into an
   * absolute estimate. A real one-input send measured 1535 bytes; this is set
   * generously above that because extra inputs add weight, and a fee estimate
   * that is too low is the failure this exists to prevent.
   */
  private static final long ESTIMATED_TX_WEIGHT = 3000L;

  private static final String DEFAULT_FEE_PRIORITY = "1";

  /**
   * Attempts before a withdrawal is treated as permanently failed. A send that
   * cannot be built - typically because the fee exceeds the amount - fails
   * identically every time, so retrying forever would strand the user's already
   * debited balance.
   */
  private static final int MAX_SEND_ATTEMPTS = 3;

  /**
   * Log a louder warning every this many blocks a withdrawal has waited for
   * locked funds. There is no give-up: the entry stays queued and is retried
   * oldest-first until the wallet can spend.
   */
  private static final long DEFERRAL_WARNING_INTERVAL_BLOCKS = 30L;

  // monero-wallet-rpc error codes, from wallet_rpc_server_error_codes.h.
  static final int RPC_WRONG_ADDRESS = -2;
  static final int RPC_TX_NOT_POSSIBLE = -16;
  static final int RPC_NOT_ENOUGH_MONEY = -17;
  static final int RPC_TX_TOO_LARGE = -18;
  static final int RPC_NOT_ENOUGH_UNLOCKED_MONEY = -37;
  static final int RPC_NO_DAEMON_CONNECTION = -38;
  static final int RPC_ZERO_AMOUNT = -46;

  /**
   * How often to re-scan the wallet's entire incoming history instead of just
   * the window around the cursor.
   *
   * <p>The windowed scan only looks a short way behind the cursor, so a deposit
   * that matures while the API is down for longer than that window would fall
   * out of range permanently. Monero gives no way to inspect a subaddress
   * balance from outside the wallet, which is how a missed Nano deposit would be
   * spotted, so this periodic full sweep is the equivalent safety net. It is
   * cheap to do rarely because crediting is idempotent.
   */
  private static final long FULL_SCAN_INTERVAL_MS = 10L * 60L * 1000L;

  /**
   * Deliberately not persisted: a restart forces a full sweep, which is exactly
   * what is wanted after downtime.
   */
  private volatile long lastFullScanMillis = 0L;

  /**
   * How often to look in the transaction pool for deposits that have not been
   * mined yet. The wallet scan proper is gated on the chain advancing, which on
   * Monero is about two minutes, but a user watching for their deposit should
   * hear about it well before that. Ten seconds keeps the wallet from being
   * polled every pass while still feeling immediate.
   */
  private static final long POOL_SCAN_INTERVAL_MS = 10L * 1000L;

  private volatile long lastPoolScanMillis = 0L;

  /**
   * Daemon height at which each waiting withdrawal was last refused for locked
   * funds, by queue id. A locked output only changes state when a block
   * arrives, so there is no point asking the wallet again before then; this
   * keeps a waiting entry to one attempt per block instead of one per second.
   * Deliberately in memory: a restart just retries immediately, which is
   * harmless.
   */
  private final Map<String, Long> deferredAtHeight = new ConcurrentHashMap<>();

  /**
   * Queue ids whose owner has been told their withdrawal is waiting. In memory
   * for the same reason; a restart may repeat the notice once.
   */
  private final Set<String> delayNoticed = ConcurrentHashMap.newKeySet();

  private static final String ACCOUNT_INDEX = "account_index";
  private static final String ADDRESS_KEY = "address";
  private static final String AMOUNT_KEY = "amount";
  private static final String TX_HASH_KEY = "tx_hash";

  private final MoneroWalletRpcClient rpc;
  private final CurrenciesService currenciesService;
  private final DepositAddressService depositAddressService;
  private final DepositRecordsRepository depositRecordsRepository;
  private final DepositNoticeService depositNoticeService;
  private final QueuesService queuesService;
  private final FileLogger fileLogger;

  @Autowired
  private ChainLedgerService chainLedgerService;

  public MoneroChainAdapter(
    MoneroWalletRpcClient rpc,
    CurrenciesService currenciesService,
    DepositAddressService depositAddressService,
    DepositRecordsRepository depositRecordsRepository,
    DepositNoticeService depositNoticeService,
    QueuesService queuesService
  ) {
    this.rpc = rpc;
    this.currenciesService = currenciesService;
    this.depositAddressService = depositAddressService;
    this.depositRecordsRepository = depositRecordsRepository;
    this.depositNoticeService = depositNoticeService;
    this.queuesService = queuesService;
    this.fileLogger = new FileLogger("MoneroChainAdapter");
  }

  @Override
  public String protocol() {
    return Constants.PROTOCOL_MONERO;
  }

  @Override
  public boolean supportsRepresentative() {
    return false;
  }

  /**
   * Asks the wallet to build the exact transaction a withdrawal would send,
   * without relaying it, and reports the fee it settled on.
   *
   * <p>{@code do_not_relay} stops short of committing, so nothing is marked
   * spent and the real send later selects afresh. The parameters are the ones
   * the real send uses, so the two cannot drift apart.
   *
   * <p>Locked funds are reported as delayed rather than rejected: the send can
   * go out once the wallet unlocks, but the user has to choose that wait. An
   * unreachable wallet is unavailable and uses the estimate. Every other
   * refusal is a verdict on the request and is surfaced before any debit.
   */
  @Override
  public FeeQuote quoteWithdrawalFee(
    CurrencyEntity currencyEntity,
    String raw,
    String address
  ) {
    JSONObject params;
    try {
      params = buildTransferParams(currencyEntity, raw, address);
    } catch (RuntimeException e) {
      fileLogger.warn(
        "Could not build a " +
        currencyEntity.getTicker() +
        " fee quote for " +
        raw +
        ": " +
        e.getMessage()
      );
      return FeeQuote.unavailable();
    }
    params.put("do_not_relay", true);

    RpcResponse response = rpc.walletDetailed(currencyEntity, "transfer", params);
    if (response.isSuccess()) {
      long fee = response.result().optLong("fee", -1L);
      FeeQuote quote = FeeQuote.ofFee(BigInteger.valueOf(fee));
      if (quote.status() == FeeQuote.Status.QUOTED) {
        fileLogger.info(
          "Quoted " +
          currencyEntity.getTicker() +
          " withdrawal of " +
          raw +
          ": fee " +
          fee +
          " (weight " +
          response.result().optLong("weight", 0L) +
          ")"
        );
      }
      return quote;
    }

    WalletRefusal refusal = classifyRefusal(currencyEntity, response);
    if (refusal.kind() == WalletRefusal.Kind.FUNDS_LOCKED) {
      return FeeQuote.delayed(refusal);
    }
    if (refusal.isTransient()) {
      return FeeQuote.unavailable();
    }
    logOperatorConcern(currencyEntity, refusal, "fee quote for " + raw);
    return FeeQuote.rejected(refusal);
  }

  /**
   * The {@code transfer} request for a withdrawal. Shared by the fee quote and
   * the real send.
   *
   * <p>The user is debited the full amount and the network fee is taken out of
   * what they receive. That keeps the ledger exactly balanced: the hot wallet
   * drops by precisely the amount burned from the user's balance.
   */
  JSONObject buildTransferParams(
    CurrencyEntity currencyEntity,
    String raw,
    String address
  ) {
    QueueEntity single = new QueueEntity();
    single.setRaw(raw);
    single.setTargetAddress(address);
    return buildTransferParams(currencyEntity, List.of(single));
  }

  /**
   * One {@code transfer} for every queued withdrawal in {@code batch}, oldest
   * first. The network fee is split across all outputs so a rain of waiting
   * sends can share one 10-block lock instead of queuing up behind each
   * other's change.
   */
  JSONObject buildTransferParams(
    CurrencyEntity currencyEntity,
    List<QueueEntity> batch
  ) {
    JSONArray destinations = new JSONArray();
    JSONArray subtractFeeFrom = new JSONArray();
    for (int i = 0; i < batch.size(); i++) {
      QueueEntity entry = batch.get(i);
      destinations.put(
        new JSONObject()
          .put(AMOUNT_KEY, new BigInteger(entry.getRaw()).longValueExact())
          .put(ADDRESS_KEY, entry.getTargetAddress())
      );
      subtractFeeFrom.put(i);
    }
    return new JSONObject()
      .put("destinations", destinations)
      .put(ACCOUNT_INDEX, 0)
      .put("priority", resolveFeePriority(currencyEntity))
      .put("ring_size", 16)
      .put("subtract_fee_from_outputs", subtractFeeFrom)
      .put("get_tx_key", true);
  }

  /**
   * Turns a failed {@code transfer} into something the rest of the adapter can
   * act on, with wording a user can be shown.
   *
   * <p>Keyed on the RPC error code, with the message text as a fallback so a
   * wallet version that numbers things differently still lands somewhere
   * sensible rather than in OTHER.
   */
  WalletRefusal classifyRefusal(
    CurrencyEntity currencyEntity,
    RpcResponse response
  ) {
    WalletRefusal.Kind kind = response.isTransportFailure()
      ? WalletRefusal.Kind.UNREACHABLE
      : classifyCode(response.errorCode(), response.errorMessage());
    String name = currencyEntity.getName();
    return switch (kind) {
      case FEE_EXCEEDS_AMOUNT -> WalletRefusal.feeExceedsAmount(
        currenciesService.formatEffectiveMinimumWithdraw(currencyEntity),
        currencyEntity.getTicker()
      );
      case FUNDS_LOCKED -> new WalletRefusal(
        kind,
        "The bot's " +
        name +
        " wallet is briefly locked while a recent transaction settles " +
        "(about 20 minutes)."
      );
      case INSUFFICIENT_FUNDS -> new WalletRefusal(
        kind,
        "The bot's " +
        name +
        " wallet cannot cover this withdrawal plus the network fee right now.",
        "Try a slightly smaller amount, or try again later."
      );
      case TX_TOO_LARGE -> new WalletRefusal(
        kind,
        "This withdrawal would need more inputs than fit in a single " +
        name +
        " transaction.",
        "Try a smaller amount, or try again later."
      );
      case ADDRESS_REJECTED -> new WalletRefusal(
        kind,
        "The " + name + " wallet rejected the destination address."
      );
      case UNREACHABLE -> new WalletRefusal(
        kind,
        "The bot's " + name + " wallet is not responding right now."
      );
      case OTHER -> new WalletRefusal(
        kind,
        "The bot's " + name + " wallet could not create this transaction."
      );
    };
  }

  static WalletRefusal.Kind classifyCode(Integer code, String message) {
    if (code != null) {
      switch (code) {
        case RPC_NOT_ENOUGH_UNLOCKED_MONEY:
          return WalletRefusal.Kind.FUNDS_LOCKED;
        case RPC_NOT_ENOUGH_MONEY:
          return WalletRefusal.Kind.INSUFFICIENT_FUNDS;
        case RPC_TX_NOT_POSSIBLE:
        case RPC_ZERO_AMOUNT:
          return WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT;
        case RPC_TX_TOO_LARGE:
          return WalletRefusal.Kind.TX_TOO_LARGE;
        case RPC_WRONG_ADDRESS:
          return WalletRefusal.Kind.ADDRESS_REJECTED;
        case RPC_NO_DAEMON_CONNECTION:
          return WalletRefusal.Kind.UNREACHABLE;
        default:
          break;
      }
    }
    String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
    if (text.contains("unlocked")) {
      return WalletRefusal.Kind.FUNDS_LOCKED;
    }
    if (text.contains("not enough money")) {
      return WalletRefusal.Kind.INSUFFICIENT_FUNDS;
    }
    if (text.contains("not possible") || text.contains("greater than")) {
      return WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT;
    }
    if (text.contains("too large")) {
      return WalletRefusal.Kind.TX_TOO_LARGE;
    }
    if (text.contains("address")) {
      return WalletRefusal.Kind.ADDRESS_REJECTED;
    }
    if (text.contains("daemon")) {
      return WalletRefusal.Kind.UNREACHABLE;
    }
    return WalletRefusal.Kind.OTHER;
  }

  /**
   * Some refusals are about the hot wallet rather than the request and need
   * an operator to look: a shortfall means liabilities exceed what the wallet
   * holds, and an oversized transaction means the outputs need consolidating.
   */
  private void logOperatorConcern(
    CurrencyEntity currencyEntity,
    WalletRefusal refusal,
    String context
  ) {
    if (
      refusal.kind() == WalletRefusal.Kind.INSUFFICIENT_FUNDS ||
      refusal.kind() == WalletRefusal.Kind.TX_TOO_LARGE
    ) {
      fileLogger.error(
        currencyEntity.getTicker() +
        " wallet refused a " +
        context +
        " with " +
        refusal.kind() +
        "; this needs attention."
      );
    }
  }

  @Override
  public String resolveDepositAddress(
    UserDetailsEntity userDetails,
    CurrencyEntity currencyEntity
  ) {
    String ticker = currencyEntity.getTicker();
    String userId = userDetails.getUserId();

    Optional<DepositAddressEntity> existing = depositAddressService.getByUser(
      ticker,
      userId
    );
    if (existing.isPresent()) {
      return existing.get().getAddress();
    }

    JSONObject result = rpc.wallet(
      currencyEntity,
      "create_address",
      new JSONObject().put(ACCOUNT_INDEX, 0).put("label", "user:" + userId)
    );
    if (result == null || !result.has(ADDRESS_KEY)) {
      fileLogger.error(
        "Could not create a " + ticker + " subaddress for user " + userId
      );
      return null;
    }

    String address = result.getString(ADDRESS_KEY);
    long addressIndex = result.getLong("address_index");
    return depositAddressService
      .record(userId, ticker, address, addressIndex)
      .getAddress();
  }

  @Override
  public void processActivity(
    UserDetailsEntity botUserDetails,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    Long daemonHeight = fetchDaemonHeight(currencyEntity);
    if (daemonHeight == null) {
      return;
    }

    if (currencyEntity.getProcessWithdrawals()) {
      processWithdrawals(currencyEntity, commandMap, daemonHeight);
    }

    if (currencyEntity.getProcessDeposits()) {
      processDeposits(currencyEntity, daemonHeight, commandMap);
    }

    if (
      currencyEntity.getProcessWithdrawals() ||
      currencyEntity.getProcessDeposits()
    ) {
      announceOwedDepositConfirms(currencyEntity, commandMap);
    }

    refreshFeeEstimate(currencyEntity);
    refreshLiquidity(currencyEntity);
  }

  /**
   * Refreshes the stored network fee estimate. Kept as its own field rather than
   * being added into minimumWithdraw, because folding it in on each refresh
   * would compound and drive the minimum up without bound.
   */
  private void refreshFeeEstimate(CurrencyEntity currencyEntity) {
    JSONObject estimate = rpc.daemon(
      currencyEntity,
      "get_fee_estimate",
      null
    );
    if (estimate == null) {
      return;
    }

    int priority = resolveFeePriority(currencyEntity);
    long perByte = estimate.optLong("fee", 0L);
    JSONArray tiers = estimate.optJSONArray("fees");
    if (tiers != null && tiers.length() >= priority) {
      perByte = tiers.optLong(priority - 1, perByte);
    }
    if (perByte <= 0L) {
      return;
    }

    long fee = perByte * ESTIMATED_TX_WEIGHT;

    // The daemon rounds fees up to a multiple of quantization_mask, so quantize
    // the same way rather than under-estimating.
    long mask = estimate.optLong("quantization_mask", 0L);
    if (mask > 1L) {
      fee = ((fee + mask - 1) / mask) * mask;
    }

    String feeString = String.valueOf(fee);
    if (!feeString.equals(currencyEntity.getFeeEstimate())) {
      currencyEntity.setFeeEstimate(feeString);
      currenciesService.updateChainState(
        currencyEntity.getId(),
        null,
        feeString,
        null
      );
    }
  }

  private int resolveFeePriority(CurrencyEntity currencyEntity) {
    long configured = ChainSettings.parseLong(
      currencyEntity.getFeePriority() == null
        ? DEFAULT_FEE_PRIORITY
        : currencyEntity.getFeePriority(),
      1L
    );
    if (configured < 1L || configured > 4L) {
      return 1;
    }
    return (int) configured;
  }

  /**
   * Daemon get_info is cheap, so it gates the expensive wallet work. Returns
   * null when the node is unreachable or still syncing, which skips the pass.
   */
  private Long fetchDaemonHeight(CurrencyEntity currencyEntity) {
    JSONObject info = rpc.daemon(currencyEntity, "get_info", null);
    if (info == null) {
      fileLogger.warn(currencyEntity.getTicker() + " daemon is unreachable");
      return null;
    }
    if (!info.optBoolean("synchronized", false)) {
      fileLogger.warn(
        currencyEntity.getTicker() + " daemon is not synchronized yet"
      );
      return null;
    }
    return info.optLong("height", 0L);
  }

  // ---------------------------------------------------------------- deposits

  private void processDeposits(
    CurrencyEntity currencyEntity,
    long daemonHeight,
    Map<String, String> commandMap
  ) {
    int confirmations = ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS);
    long cursor = ChainSettings.parseLong(currencyEntity.getLastScannedHeight(), 0L);
    long safeHeight = daemonHeight - confirmations;

    boolean fullScan =
      System.currentTimeMillis() - lastFullScanMillis >= FULL_SCAN_INTERVAL_MS;

    // Unmined deposits are announced from the pool independently of the block
    // gated scan below, so the user hears about a deposit when it is sent rather
    // than when the next block lands.
    announcePoolDeposits(currencyEntity, confirmations, commandMap);

    // Nothing can have matured since the last pass, and no sweep is due.
    if (!fullScan && safeHeight <= cursor) {
      return;
    }

    // A full sweep ignores the cursor entirely and re-offers every incoming
    // transfer the wallet knows about. Anything already credited is rejected by
    // the depositRecords unique index, so this can only fill in gaps.
    long scanFrom = fullScan ? 0L : Math.max(0L, cursor - REORG_LOOKBACK);
    JSONObject result = rpc.wallet(
      currencyEntity,
      "get_transfers",
      new JSONObject()
        .put("in", true)
        // Credit-only safety net for a self-send whose queue was dropped
        // before the ledger row was written. Notices for those stay on
        // confirmSend; this path never announces.
        .put("out", true)
        .put("pending", false)
        .put("pool", false)
        .put("filter_by_height", true)
        .put("min_height", scanFrom)
        .put("max_height", daemonHeight)
    );
    if (result == null) {
      return;
    }

    JSONArray incoming = result.optJSONArray("in");
    if (incoming != null) {
      for (int i = 0; i < incoming.length(); i++) {
        creditDeposit(
          currencyEntity,
          incoming.getJSONObject(i),
          confirmations,
          commandMap
        );
      }
    }

    JSONArray outgoing = result.optJSONArray("out");
    if (outgoing != null) {
      for (int i = 0; i < outgoing.length(); i++) {
        creditOutgoingIfUnbooked(
          currencyEntity,
          outgoing.getJSONObject(i),
          confirmations,
          commandMap
        );
      }
    }

    if (fullScan) {
      lastFullScanMillis = System.currentTimeMillis();
      fileLogger.info(
        "Completed a full " +
        currencyEntity.getTicker() +
        " deposit reconciliation sweep."
      );
    }

    // Advance only to the matured height so an output is never skipped before
    // it has enough confirmations to be credited.
    if (safeHeight > cursor) {
      currenciesService.updateChainState(
        currencyEntity.getId(),
        null,
        null,
        String.valueOf(safeHeight)
      );
    }
  }

  private void creditDeposit(
    CurrencyEntity currencyEntity,
    JSONObject transfer,
    int confirmations,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();
    String txid = transfer.optString("txid", null);
    if (txid == null) {
      return;
    }

    long depth = transfer.optLong("confirmations", 0L);
    if (depth < confirmations) {
      // Mined but not yet deep enough. Usually already announced from the
      // pool; this catches a deposit the pool look missed, say across a
      // restart, so nobody waits ten blocks in silence.
      announceDeposit(currencyEntity, transfer, depth, confirmations, commandMap);
      return;
    }
    // unlock_time is a separate lock the sender can set on top of the standard
    // 10 block maturity, and those funds are genuinely unspendable until it
    // passes.
    if (transfer.optLong("unlock_time", 0L) > 0L) {
      fileLogger.warn(
        "Skipping " + ticker + " deposit " + txid + " with a sender unlock_time"
      );
      return;
    }

    long minor = subaddressMinor(transfer);
    if (minor <= 0L) {
      return;
    }

    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        txid,
        minor
      )
    ) {
      return;
    }

    Optional<DepositAddressEntity> owner =
      depositAddressService.getByAddressIndex(ticker, minor);
    if (owner.isEmpty()) {
      fileLogger.error(
        "Received " +
        ticker +
        " deposit " +
        txid +
        " on unassigned subaddress index " +
        minor
      );
      return;
    }

    BigInteger amount = BigInteger.valueOf(transfer.optLong(AMOUNT_KEY, 0L));
    if (amount.signum() <= 0) {
      return;
    }

    creditToOwner(
      currencyEntity,
      owner.get(),
      txid,
      amount,
      String.valueOf(transfer.optLong("height", 0L)),
      commandMap
    );
  }

  /**
   * The subaddress minor index a transfer paid, or -1 when absent. Index 0 is
   * the wallet's own base address, not any user's deposit address, so funds
   * there are house funds rather than a deposit; callers treat anything not
   * strictly positive as "not a user deposit".
   */
  private static long subaddressMinor(JSONObject transfer) {
    JSONObject subaddressIndex = transfer.optJSONObject("subaddr_index");
    if (subaddressIndex == null) {
      return -1L;
    }
    return subaddressIndex.optLong("minor", -1L);
  }

  /**
   * Announces deposits still sitting in the transaction pool.
   *
   * <p>Throttled to {@link #POOL_SCAN_INTERVAL_MS} because the wallet has to
   * ask the daemon for its pool each time. The pool bucket is not subject to
   * the height filter the main scan uses, so this is a separate, narrow call.
   */
  private void announcePoolDeposits(
    CurrencyEntity currencyEntity,
    int confirmations,
    Map<String, String> commandMap
  ) {
    long now = System.currentTimeMillis();
    if (now - lastPoolScanMillis < POOL_SCAN_INTERVAL_MS) {
      return;
    }
    lastPoolScanMillis = now;

    JSONObject result = rpc.wallet(
      currencyEntity,
      "get_transfers",
      new JSONObject().put("pool", true).put(ACCOUNT_INDEX, 0)
    );
    if (result == null) {
      return;
    }
    JSONArray pool = result.optJSONArray("pool");
    if (pool == null) {
      return;
    }
    for (int i = 0; i < pool.length(); i++) {
      announceDeposit(
        currencyEntity,
        pool.getJSONObject(i),
        0L,
        confirmations,
        commandMap
      );
    }
  }

  /**
   * Tells the subaddress owner about an incoming transfer that has been seen but
   * not yet credited, once per deposit.
   *
   * <p>Skipped when a deposit record already exists, which means it was credited
   * on an earlier pass: a "discovered" after a "confirmed" would read as a second
   * deposit. Transfers with a sender unlock_time are skipped too, because the
   * credit path refuses them and announcing would promise funds that will not
   * be credited on schedule. The notice store makes the announcement itself
   * exactly-once, so re-seeing the transfer on every pass costs nothing.
   */
  private void announceDeposit(
    CurrencyEntity currencyEntity,
    JSONObject transfer,
    long depth,
    int confirmations,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();
    String txid = transfer.optString("txid", null);
    if (txid == null) {
      return;
    }
    if (transfer.optLong("unlock_time", 0L) > 0L) {
      return;
    }

    long minor = subaddressMinor(transfer);
    if (minor <= 0L) {
      return;
    }

    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        txid,
        minor
      )
    ) {
      return;
    }

    Optional<DepositAddressEntity> owner =
      depositAddressService.getByAddressIndex(ticker, minor);
    if (owner.isEmpty()) {
      return;
    }

    BigInteger amount = BigInteger.valueOf(transfer.optLong(AMOUNT_KEY, 0L));
    if (amount.signum() <= 0) {
      return;
    }

    String userId = owner.get().getUserId();
    if (
      !depositNoticeService.recordFirstSighting(
        ticker,
        txid,
        minor,
        userId,
        amount.toString()
      )
    ) {
      return;
    }

    chainLedgerService.notifyDepositDiscovered(
      currencyEntity,
      userId,
      amount.toString(),
      txid,
      owner.get().getAddress(),
      depth,
      confirmations,
      commandMap
    );

    fileLogger.info(
      "Discovered " +
      amount +
      " " +
      ticker +
      " for user " +
      userId +
      " in " +
      txid +
      " (" +
      depth +
      " of " +
      confirmations +
      " confirmations)"
    );
  }

  /**
   * Same announcement as {@link #announceDeposit}, for an outgoing transfer
   * whose destinations include one of our deposit addresses. wallet-rpc never
   * reports those as {@code in} or {@code pool}, so the outgoing side is the
   * only sighting. Amounts to the same address are summed so a combined send
   * is one discovery, matching the one credit.
   */
  private void announceInternalDeposits(
    CurrencyEntity currencyEntity,
    JSONObject transfer,
    long depth,
    int confirmations,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();
    String txid = transfer.optString("txid", null);
    if (txid == null || txid.isBlank()) {
      return;
    }
    if (transfer.optLong("unlock_time", 0L) > 0L) {
      return;
    }

    for (InternalDeposit dest : internalDeposits(ticker, transfer)) {
      Long addressIndex = dest.owner().getAddressIndex();
      if (
        depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
          ticker,
          txid,
          addressIndex
        )
      ) {
        continue;
      }
      String userId = dest.owner().getUserId();
      if (
        !depositNoticeService.recordFirstSighting(
          ticker,
          txid,
          addressIndex,
          userId,
          dest.amount().toString()
        )
      ) {
        continue;
      }

      chainLedgerService.notifyDepositDiscovered(
        currencyEntity,
        userId,
        dest.amount().toString(),
        txid,
        dest.owner().getAddress(),
        depth,
        confirmations,
        commandMap
      );

      fileLogger.info(
        "Discovered " +
        dest.amount() +
        " " +
        ticker +
        " for user " +
        userId +
        " in outgoing " +
        txid +
        " (" +
        depth +
        " of " +
        confirmations +
        " confirmations)"
      );
    }
  }

  /**
   * Credits a mature outgoing transfer that paid one of our deposit addresses
   * if {@link #confirmSend} never wrote the row. No notices: those belong to
   * the withdrawal confirm path.
   */
  private void creditOutgoingIfUnbooked(
    CurrencyEntity currencyEntity,
    JSONObject transfer,
    int confirmations,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();
    String txid = transfer.optString("txid", null);
    if (txid == null) {
      return;
    }
    if (transfer.optLong("unlock_time", 0L) > 0L) {
      return;
    }
    if (transfer.optLong("confirmations", 0L) < confirmations) {
      return;
    }

    String height = String.valueOf(transfer.optLong("height", 0L));
    for (InternalDeposit dest : internalDeposits(ticker, transfer)) {
      bookDeposit(
        currencyEntity,
        dest.owner(),
        txid,
        dest.amount(),
        height,
        commandMap,
        false
      );
    }
  }

  /**
   * Books a deposit to the address owner exactly once.
   *
   * <p>The depositRecords row is inserted before the credit so its unique index
   * on (ticker, txid, addressIndex) is what enforces exactly-once. A concurrent
   * or repeated scan fails the insert and returns instead of crediting again,
   * which is what makes the periodic full reconciliation sweep safe to run.
   */
  private void creditToOwner(
    CurrencyEntity currencyEntity,
    DepositAddressEntity owner,
    String txid,
    BigInteger amount,
    String height,
    Map<String, String> commandMap
  ) {
    bookDeposit(
      currencyEntity,
      owner,
      txid,
      amount,
      height,
      commandMap,
      true
    );
  }

  // ------------------------------------------------------------- withdrawals

  private void processWithdrawals(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap,
    long daemonHeight
  ) {
    String ticker = currencyEntity.getTicker();
    // Oldest first so a waiting withdrawal is not skipped in favour of a newer
    // one. Unsent entries are gathered and sent as one multi-destination
    // transfer when the wallet can cover them together, so several users
    // waiting on the same locked change share one 10-block wait instead of
    // queuing up behind each other's change.
    List<QueueEntity> pending = new ArrayList<>();
    boolean unlockedThisPass = false;
    for (QueueEntity queueEntity : queuesService.getQueuesByTicker(ticker)) {
      if (queueEntity.getLevel() == LevelDto.UPDATE) {
        // Monero has no representative to set. Such an entry can only be a
        // misconfiguration, so drop it rather than leave it queued forever.
        fileLogger.error(
          "Discarding representative update queued for " +
          ticker +
          ", which has no representatives - queue #" +
          queueEntity.getId()
        );
        queuesService.deleteQueue(queueEntity.getId());
        continue;
      }
      if (queueEntity.getLevel() != LevelDto.SEND) {
        continue;
      }
      if (queueEntity.getProcessed()) {
        if (confirmSend(queueEntity, currencyEntity, commandMap)) {
          unlockedThisPass = true;
        }
      } else {
        pending.add(queueEntity);
      }
    }
    // A just-confirmed send unlocks its change. Retry waiting withdrawals in
    // this same pass instead of waiting for the next block.
    if (unlockedThisPass) {
      deferredAtHeight.clear();
    }
    processPendingSends(pending, currencyEntity, daemonHeight, commandMap);
  }

  /**
   * Sends every ready withdrawal that will fit in one transfer, shrinking the
   * oldest-first prefix until the wallet accepts it. A full lock defers the
   * remainder together so they retry as a group when the next block lands.
   */
  private void processPendingSends(
    List<QueueEntity> pending,
    CurrencyEntity currencyEntity,
    long daemonHeight,
    Map<String, String> commandMap
  ) {
    List<QueueEntity> ready = new ArrayList<>();
    for (QueueEntity queueEntity : pending) {
      if (!prepareSend(queueEntity, currencyEntity, daemonHeight)) {
        continue;
      }
      ready.add(queueEntity);
    }

    while (!ready.isEmpty()) {
      int n = ready.size();
      RpcResponse response = null;
      while (n >= 1) {
        List<QueueEntity> batch = ready.subList(0, n);
        JSONObject params;
        try {
          params = buildTransferParams(currencyEntity, batch);
        } catch (RuntimeException e) {
          if (n > 1) {
            n--;
            continue;
          }
          fileLogger.error(
            "Could not build a " +
            currencyEntity.getTicker() +
            " transfer for queue #" +
            ready.get(0).getId() +
            ": " +
            e.getMessage()
          );
          handleSendFailure(
            ready.get(0),
            currencyEntity,
            new WalletRefusal(
              WalletRefusal.Kind.OTHER,
              "The bot's " +
              currencyEntity.getName() +
              " wallet could not create this transaction."
            ),
            commandMap
          );
          ready.remove(0);
          response = null;
          break;
        }

        response = rpc.walletDetailed(currencyEntity, "transfer", params);
        if (response.isSuccess()) {
          break;
        }

        WalletRefusal refusal = classifyRefusal(currencyEntity, response);
        if (refusal.kind() == WalletRefusal.Kind.UNREACHABLE) {
          fileLogger.warn(
            "Could not reach the " +
            currencyEntity.getTicker() +
            " wallet for queue #" +
            batch.get(0).getId() +
            "; will retry."
          );
          return;
        }
        if (n == 1) {
          if (refusal.kind() == WalletRefusal.Kind.FUNDS_LOCKED) {
            for (QueueEntity waiting : ready) {
              deferSend(waiting, currencyEntity, daemonHeight, commandMap);
            }
            return;
          }
          handleSendFailure(ready.get(0), currencyEntity, refusal, commandMap);
          ready.remove(0);
          response = null;
          break;
        }
        n--;
      }

      if (response == null || !response.isSuccess()) {
        continue;
      }

      String txHash = response.resultString(TX_HASH_KEY);
      if (txHash == null) {
        fileLogger.warn(
          currencyEntity.getTicker() +
          " wallet accepted the transfer for queue #" +
          ready.get(0).getId() +
          " but returned no tx_hash; will retry."
        );
        return;
      }

      JSONObject result = response.result();
      List<QueueEntity> sent = new ArrayList<>(ready.subList(0, n));
      List<BigInteger> feeShares = splitFee(broadcastFee(result), sent.size());
      for (int i = 0; i < sent.size(); i++) {
        recordBroadcast(
          sent.get(i),
          currencyEntity,
          txHash,
          result.optLong("fee", 0L),
          feeShares.get(i),
          commandMap
        );
      }
      announceBroadcastDeposits(
        sent,
        currencyEntity,
        txHash,
        feeShares,
        commandMap
      );
      ready.subList(0, n).clear();
    }
  }

  /**
   * Recovery, the per-block deferral gate, and the attempt-height stamp.
   * False means this entry is not a candidate for a send on this pass.
   */
  private boolean prepareSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long daemonHeight
  ) {
    // Recover before sending again. If a previous attempt broadcast a
    // transaction but died before the hash was saved, sending now would pay the
    // user twice.
    if (queueEntity.getBlockHash() == null && queueEntity.getIndex() != null) {
      BroadcastCheck check = findAlreadySentTx(
        queueEntity,
        currencyEntity,
        queueEntity.getIndex()
      );
      if (!check.conclusive()) {
        fileLogger.warn(
          "Cannot verify whether queue #" +
          queueEntity.getId() +
          " was already broadcast; deferring rather than risking a double send."
        );
        return false;
      }
      if (check.txid() != null) {
        String recovered = check.txid();
        fileLogger.warn(
          "Recovered an in-flight " +
          currencyEntity.getTicker() +
          " withdrawal instead of resending: " +
          recovered
        );
        queueEntity.setBlockHash(recovered);
        queueEntity.setProcessed(true);
        queuesService.updateQueueProgress(
          queueEntity.getId(),
          null,
          null,
          recovered,
          true
        );
        return false;
      }
    }

    Long deferredAt = deferredAtHeight.get(queueEntity.getId());
    if (deferredAt != null && daemonHeight <= deferredAt) {
      return false;
    }

    if (queueEntity.getIndex() == null) {
      queueEntity.setIndex(daemonHeight);
      queuesService.updateQueueProgress(
        queueEntity.getId(),
        null,
        daemonHeight,
        null,
        null
      );
    }
    return true;
  }

  private void recordBroadcast(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    String txHash,
    long totalFee,
    BigInteger feeShare,
    Map<String, String> commandMap
  ) {
    clearDeferral(queueEntity.getId());
    queueEntity.setBlockHash(txHash);
    queueEntity.setProcessed(true);
    queuesService.updateQueueProgress(
      queueEntity.getId(),
      null,
      null,
      queueEntity.getBlockHash(),
      true
    );

    fileLogger.info(
      "Submitted " +
      currencyEntity.getTicker() +
      " withdrawal " +
      queueEntity.getBlockHash() +
      " for queue #" +
      queueEntity.getId() +
      " (fee " +
      totalFee +
      " deducted from the amount sent)"
    );

    chainLedgerService.notifyWithdrawalSent(
      currencyEntity,
      queueEntity,
      ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS),
      commandMap,
      feeShare
    );
  }

  /**
   * Holds a withdrawal the wallet refused for lack of unlocked funds.
   *
   * <p>Nothing is wrong with the request: the wallet holds the money, some of
   * it is just inside the 10-block lock that follows every send, usually the
   * change from the withdrawal before this one. The entry is left queued and
   * asked again when the next block lands, and its owner is told once that it
   * is waiting rather than lost. Attempts are not counted, because counting
   * them would turn a normal wait into a refund with a misleading reason.
   * There is no time bound: older entries stay at the front of the queue
   * and are sent together with whoever else is waiting, in one transfer,
   * once the wallet can spend.
   */
  private void deferSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long daemonHeight,
    Map<String, String> commandMap
  ) {
    String id = queueEntity.getId();
    long since = queueEntity.getIndex() == null
      ? daemonHeight
      : queueEntity.getIndex();
    long waited = Math.max(0L, daemonHeight - since);

    deferredAtHeight.put(id, daemonHeight);
    String snapshot = balanceSnapshot(currencyEntity);

    if (delayNoticed.add(id)) {
      chainLedgerService.notifyWithdrawalDelayed(
        currencyEntity,
        queueEntity,
        "Monero locks funds for 10 blocks (about 20 minutes) after every " +
        "send, and the bot's wallet is waiting for a recent transaction to " +
        "unlock. Your withdrawal will be sent automatically once it does.",
        commandMap
      );
      fileLogger.warn(
        "Deferring " +
        currencyEntity.getTicker() +
        " withdrawal for queue #" +
        id +
        ": not enough unlocked funds (" +
        snapshot +
        "). Will retry as blocks arrive."
      );
      return;
    }

    if (waited > 0 && waited % DEFERRAL_WARNING_INTERVAL_BLOCKS == 0) {
      fileLogger.warn(
        currencyEntity.getTicker() +
        " withdrawal for queue #" +
        id +
        " still waiting after " +
        waited +
        " blocks (" +
        snapshot +
        ")."
      );
    } else {
      fileLogger.info(
        "Queue #" +
        id +
        " still waiting for unlocked " +
        currencyEntity.getTicker() +
        " at height " +
        daemonHeight +
        " (" +
        snapshot +
        ")."
      );
    }
  }

  /** "unlocked X of Y TICKER, N blocks to unlock", for the deferral log lines. */
  private String balanceSnapshot(CurrencyEntity currencyEntity) {
    JSONObject balance = rpc.wallet(
      currencyEntity,
      "get_balance",
      new JSONObject().put(ACCOUNT_INDEX, 0)
    );
    if (balance == null) {
      return "balance unavailable";
    }
    int precision = Integer.parseInt(currencyEntity.getPrecision());
    return (
      "unlocked " +
      currenciesService.getCurrencyDecimalValue(
        String.valueOf(balance.optLong("unlocked_balance", 0L)),
        precision
      ) +
      " of " +
      currenciesService.getCurrencyDecimalValue(
        String.valueOf(balance.optLong("balance", 0L)),
        precision
      ) +
      " " +
      currencyEntity.getTicker() +
      ", " +
      balance.optLong("blocks_to_unlock", 0L) +
      " blocks to unlock"
    );
  }

  private void clearDeferral(String queueId) {
    deferredAtHeight.remove(queueId);
    delayNoticed.remove(queueId);
  }

  /**
   * Counts a refused send attempt and, once attempts are exhausted, refunds the
   * user with the wallet's reason rather than retrying forever.
   *
   * <p>Only refusals reach here: a wallet that did not answer is not counted,
   * and a wallet short of unlocked funds is deferred instead. What is left
   * fails identically on every retry - a fee larger than the amount, a wallet
   * that does not hold enough, a transaction that would be too large - and
   * since the balance was debited when the withdrawal was queued, leaving it
   * queued would strand the user's funds indefinitely.
   */
  private void handleSendFailure(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    WalletRefusal refusal,
    Map<String, String> commandMap
  ) {
    int attempts =
      (queueEntity.getAttempts() == null ? 0 : queueEntity.getAttempts()) + 1;
    queueEntity.setAttempts(attempts);
    queuesService.updateQueueProgress(
      queueEntity.getId(),
      attempts,
      null,
      null,
      null
    );

    fileLogger.error(
      "Failed to submit " +
      currencyEntity.getTicker() +
      " withdrawal for queue #" +
      queueEntity.getId() +
      " (" +
      refusal.kind() +
      ", attempt " +
      attempts +
      " of " +
      MAX_SEND_ATTEMPTS +
      ")"
    );

    if (attempts < MAX_SEND_ATTEMPTS) {
      return;
    }

    logOperatorConcern(
      currencyEntity,
      refusal,
      "withdrawal for queue #" + queueEntity.getId()
    );
    refundIfNotBroadcast(
      queueEntity,
      currencyEntity,
      refusal.refundMessage(),
      commandMap
    );
  }

  /**
   * Refunds a withdrawal, but only after proving nothing reached the network;
   * otherwise the user would be credited for a withdrawal they also received.
   * An inconclusive check holds the entry for review instead.
   */
  private void refundIfNotBroadcast(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    String reason,
    Map<String, String> commandMap
  ) {
    BroadcastCheck check = findAlreadySentTx(
      queueEntity,
      currencyEntity,
      ChainSettings.parseLong(
        queueEntity.getIndex() == null
          ? null
          : String.valueOf(queueEntity.getIndex()),
        0L
      )
    );

    if (!check.conclusive()) {
      // Hold the entry and keep retrying the check. An unverifiable state is not
      // grounds for handing money back.
      fileLogger.error(
        "Withdrawal for queue #" +
        queueEntity.getId() +
        " cannot be sent, but the wallet cannot confirm whether it was " +
        "broadcast. Holding for manual review rather than refunding blind."
      );
      return;
    }

    if (check.txid() != null) {
      fileLogger.warn(
        "Withdrawal for queue #" +
        queueEntity.getId() +
        " did reach the network as " +
        check.txid() +
        "; recording it instead of refunding."
      );
      clearDeferral(queueEntity.getId());
      queueEntity.setBlockHash(check.txid());
      queueEntity.setProcessed(true);
      queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));
      return;
    }

    String refundTransactionId = chainLedgerService.refundFailedWithdrawal(
      currencyEntity,
      queueEntity,
      reason,
      commandMap
    );

    // Only drop the queue entry once the refund is booked. If it failed the
    // entry stays so the discrepancy stays visible instead of vanishing.
    if (refundTransactionId != null) {
      clearDeferral(queueEntity.getId());
      queuesService.deleteQueue(queueEntity.getId());
    }
  }

  /**
   * Looks for an outgoing transfer matching this queue entry, used to tell "the
   * send never happened" apart from "the send happened but was not recorded".
   *
   * <p>Matching has to add the fee back before comparing. Sends are built with
   * subtract_fee_from_outputs, so the wallet reports the destination as what
   * actually landed, which is the queued amount minus the fee, while the queue
   * holds the gross. Comparing those two directly never matches, which made
   * every real broadcast look like it had never happened: the recovery path
   * would resend and the failure path would refund, both paying the user twice.
   * The gross form is accepted as well so a wallet that reports the requested
   * amount still matches.
   */
  private BroadcastCheck findAlreadySentTx(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long fromHeight
  ) {
    JSONObject result = rpc.wallet(
      currencyEntity,
      "get_transfers",
      new JSONObject()
        .put("out", true)
        .put("pending", true)
        .put("pool", true)
        .put("filter_by_height", true)
        .put("min_height", Math.max(0L, fromHeight - REORG_LOOKBACK))
    );
    if (result == null) {
      return BroadcastCheck.unknown();
    }
    BigInteger expected = new BigInteger(queueEntity.getRaw());
    for (String bucket : List.of("out", "pending", "pool")) {
      JSONArray transfers = result.optJSONArray(bucket);
      if (transfers == null) {
        continue;
      }
      for (int i = 0; i < transfers.length(); i++) {
        JSONObject transfer = transfers.getJSONObject(i);
        JSONArray destinations = transfer.optJSONArray("destinations");
        if (destinations == null) {
          continue;
        }
        BigInteger fee = BigInteger.valueOf(transfer.optLong("fee", 0L));
        for (int d = 0; d < destinations.length(); d++) {
          JSONObject destination = destinations.getJSONObject(d);
          boolean sameAddress = queueEntity
            .getTargetAddress()
            .equals(destination.optString(ADDRESS_KEY, null));
          if (!sameAddress) {
            continue;
          }
          BigInteger received = BigInteger.valueOf(
            destination.optLong(AMOUNT_KEY, -1L)
          );
          if (received.signum() < 0) {
            continue;
          }
          if (
            matchesQueuedAmount(expected, received, fee)
          ) {
            return BroadcastCheck.sent(transfer.optString("txid", null));
          }
        }
      }
    }
    return BroadcastCheck.notSent();
  }

  /**
   * Whether a reported destination belongs to a queued withdrawal of
   * {@code expected}.
   *
   * <p>The destination is net of this output's share of the fee. A solo send
   * subtracts the whole fee, so {@code received + fee == expected}. A combined
   * send splits the fee across outputs, so the share is between zero and the
   * full fee. Gross is accepted too ({@code received == expected}) so the check
   * does not depend on subtract_fee_from_outputs staying true.
   */
  static boolean matchesQueuedAmount(
    BigInteger expected,
    BigInteger received,
    BigInteger fee
  ) {
    if (expected == null || received == null || fee == null) {
      return false;
    }
    if (received.signum() < 0) {
      return false;
    }
    BigInteger delta = expected.subtract(received);
    return delta.signum() >= 0 && delta.compareTo(fee) <= 0;
  }

  /**
   * Splits a combined-transaction fee the way wallet2 does: equal shares,
   * remainder on the last output. A missing or zero fee leaves each notice
   * without a figure rather than claiming nobody paid one.
   */
  static List<BigInteger> splitFee(BigInteger fee, int n) {
    List<BigInteger> shares = new ArrayList<>();
    if (n <= 0) {
      return shares;
    }
    if (fee == null || fee.signum() <= 0) {
      for (int i = 0; i < n; i++) {
        shares.add(null);
      }
      return shares;
    }
    BigInteger count = BigInteger.valueOf(n);
    BigInteger each = fee.divide(count);
    BigInteger remainder = fee.remainder(count);
    for (int i = 0; i < n - 1; i++) {
      shares.add(each);
    }
    shares.add(each.add(remainder));
    return shares;
  }

  static BigInteger broadcastFee(JSONObject transfer) {
    if (transfer == null) {
      return null;
    }
    long fee = transfer.optLong("fee", 0L);
    return fee > 0L ? BigInteger.valueOf(fee) : null;
  }

  /**
   * True when this queue reached the required confirmations and was closed.
   * A just-confirmed send unlocks change, so the caller can retry waiting
   * withdrawals in the same pass.
   */
  private boolean confirmSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    JSONObject result = rpc.wallet(
      currencyEntity,
      "get_transfer_by_txid",
      new JSONObject()
        .put("txid", queueEntity.getBlockHash())
        .put(ACCOUNT_INDEX, 0)
    );
    if (result == null) {
      return false;
    }
    JSONObject transfer = result.optJSONObject("transfer");
    if (transfer == null) {
      return false;
    }
    if (
      transfer.optLong("confirmations", 0L) <
      ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS)
    ) {
      return false;
    }

    // Credit before dropping the queue so a crash in between is recoverable.
    // Deposit Confirmed waits only until every withdrawal on this tx has
    // been announced, then goes out in this same call.
    DeferredDepositNotice deferred = creditInternalDestination(
      currencyEntity,
      queueEntity,
      transfer,
      commandMap
    );

    Optional<QueueEntity> deleted = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deleted.isPresent()) {
      chainLedgerService.notifyWithdrawalConfirmed(
        currencyEntity,
        queueEntity,
        commandMap,
        destinationFeeShare(transfer, queueEntity)
      );
    }

    DeferredDepositNotice owed = deferred != null
      ? deferred
      : noticeFromExisting(currencyEntity, queueEntity, transfer);
    if (
      owed != null &&
      !hasOpenWithdrawal(
        currencyEntity.getTicker(),
        owed.txid(),
        queueEntity.getId()
      ) &&
      depositNoticeService.recordConfirmSent(
        currencyEntity.getTicker(),
        owed.txid(),
        owed.addressIndex(),
        owed.userId(),
        owed.raw()
      )
    ) {
      chainLedgerService.notifyDepositConfirmed(
        currencyEntity,
        owed.userId(),
        owed.raw(),
        owed.txid(),
        owed.address(),
        owed.transactionId(),
        commandMap
      );
    }
    return true;
  }

  /**
   * True when another SEND queue still belongs to this on-chain transaction.
   * The deposit confirm waits until every withdrawal confirm for that
   * transaction has gone out. {@code exceptId} is the queue just confirmed, so
   * a stale read of that row cannot hold the notice forever.
   */
  private boolean hasOpenWithdrawal(
    String ticker,
    String txid,
    String exceptId
  ) {
    if (txid == null) {
      return false;
    }
    for (QueueEntity other : queuesService.getQueuesByTicker(ticker)) {
      if (exceptId != null && exceptId.equals(other.getId())) {
        continue;
      }
      if (other.getLevel() != LevelDto.SEND) {
        continue;
      }
      if (txid.equals(other.getBlockHash())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Deposit Confirmed still owed after the withdrawal queues for that
   * transaction are gone. confirmSend is the first attempt; this is how a
   * skipped last-sibling notice is not lost once the credit exists.
   */
  private void announceOwedDepositConfirms(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();
    for (DepositNoticeEntity notice : depositNoticeService.unconfirmed(ticker)) {
      if (hasOpenWithdrawal(ticker, notice.getTxid(), null)) {
        continue;
      }
      Optional<DepositRecordEntity> record =
        depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
          ticker,
          notice.getTxid(),
          notice.getAddressIndex()
        );
      if (
        record == null ||
        record.isEmpty() ||
        record.get().getTransactionId() == null
      ) {
        continue;
      }
      Optional<DepositAddressEntity> owner =
        depositAddressService.getByAddressIndex(
          ticker,
          notice.getAddressIndex()
        );
      if (owner == null || owner.isEmpty()) {
        continue;
      }
      if (
        !depositNoticeService.recordConfirmSent(
          ticker,
          notice.getTxid(),
          notice.getAddressIndex(),
          notice.getUserId(),
          record.get().getRaw()
        )
      ) {
        continue;
      }
      chainLedgerService.notifyDepositConfirmed(
        currencyEntity,
        notice.getUserId(),
        record.get().getRaw(),
        notice.getTxid(),
        owner.get().getAddress(),
        record.get().getTransactionId(),
        commandMap
      );
    }
  }

  /**
   * A deposit that has been credited on the ledger but whose user notice may
   * still be waiting on sibling withdrawals or the durable confirm flag.
   */
  private record DeferredDepositNotice(
    String userId,
    String raw,
    String txid,
    String address,
    Long addressIndex,
    String transactionId
  ) {}

  private record InternalDeposit(DepositAddressEntity owner, BigInteger amount) {}

  /**
   * Credits the owner of a deposit address that received one of our own
   * withdrawals, and returns the notice still owed to them, or null when nothing
   * was credited.
   *
   * <p>A combined send can pay the same deposit address several times in one
   * transaction; those outputs share a depositRecords key, so this sums every
   * destination to the address and books it once. Later queue entries for the
   * same tx see the record and return the same notice. The caller announces
   * after the last withdrawal on that transaction.
   */
  private DeferredDepositNotice creditInternalDestination(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    JSONObject transfer,
    Map<String, String> commandMap
  ) {
    DeferredDepositNotice already = noticeFromExisting(
      currencyEntity,
      queueEntity,
      transfer
    );
    if (already != null) {
      return already;
    }

    Optional<DepositAddressEntity> recipient = recipientOf(
      currencyEntity.getTicker(),
      queueEntity,
      transfer
    );
    if (recipient.isEmpty()) {
      return null;
    }

    String txid = queueEntity.getBlockHash();
    BigInteger received = resolveDestinationAmount(
      transfer,
      recipient.get().getAddress()
    );
    if (received == null || received.signum() <= 0) {
      fileLogger.error(
        "Could not determine the amount received by " +
        recipient.get().getAddress() +
        " in " +
        txid +
        "; the recipient has not been credited."
      );
      return null;
    }

    DepositRecordEntity record = bookDeposit(
      currencyEntity,
      recipient.get(),
      txid,
      received,
      String.valueOf(transfer.optLong("height", 0L)),
      commandMap,
      false
    );
    return noticeFrom(record, recipient.get().getAddress());
  }

  /**
   * The notice for an already-credited self-send. Used when this sibling cannot
   * resolve destinations but an earlier one (or the outgoing scan) already
   * booked the sum.
   */
  private DeferredDepositNotice noticeFromExisting(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    JSONObject transfer
  ) {
    Optional<DepositAddressEntity> recipient = recipientOf(
      currencyEntity.getTicker(),
      queueEntity,
      transfer
    );
    if (recipient.isEmpty()) {
      return null;
    }
    Optional<DepositRecordEntity> existing =
      depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
        currencyEntity.getTicker(),
        queueEntity.getBlockHash(),
        recipient.get().getAddressIndex()
      );
    if (existing == null || existing.isEmpty()) {
      return null;
    }
    return noticeFrom(existing.get(), recipient.get().getAddress());
  }

  private Optional<DepositAddressEntity> recipientOf(
    String ticker,
    QueueEntity queueEntity,
    JSONObject transfer
  ) {
    Optional<DepositAddressEntity> recipient = ownerOf(
      ticker,
      queueEntity.getTargetAddress()
    );
    if (recipient.isPresent()) {
      return recipient;
    }
    for (InternalDeposit dest : internalDeposits(ticker, transfer)) {
      return Optional.of(dest.owner());
    }
    return Optional.empty();
  }

  /**
   * Inserts the deposit record and credits the ledger exactly once. A racing
   * insert reloads the existing row instead of dropping the notice.
   */
  private DepositRecordEntity bookDeposit(
    CurrencyEntity currencyEntity,
    DepositAddressEntity owner,
    String txid,
    BigInteger amount,
    String height,
    Map<String, String> commandMap,
    boolean notify
  ) {
    String ticker = currencyEntity.getTicker();
    Long addressIndex = owner.getAddressIndex();
    Optional<DepositRecordEntity> existing =
      depositRecordsRepository.findByTickerAndTxidAndAddressIndex(
        ticker,
        txid,
        addressIndex
      );
    if (existing != null && existing.isPresent()) {
      return existing.get();
    }

    DepositRecordEntity record = new DepositRecordEntity(
      ticker,
      txid,
      addressIndex,
      owner.getUserId(),
      amount.toString(),
      height
    );
    record.setId(new ObjectId().toHexString());
    try {
      depositRecordsRepository.insert(record);
    } catch (DuplicateKeyException e) {
      return depositRecordsRepository
        .findByTickerAndTxidAndAddressIndex(ticker, txid, addressIndex)
        .orElse(null);
    }

    String transactionId = chainLedgerService.creditDeposit(
      currencyEntity,
      owner.getUserId(),
      amount.toString(),
      txid,
      owner.getAddress(),
      commandMap,
      notify
    );
    record.setTransactionId(transactionId);
    depositRecordsRepository.save(record);

    if (transactionId != null) {
      fileLogger.info(
        "Credited " +
        amount +
        " " +
        ticker +
        " to user " +
        owner.getUserId() +
        " from " +
        txid
      );
      if (notify) {
        depositNoticeService.recordConfirmSent(
          ticker,
          txid,
          addressIndex,
          owner.getUserId(),
          amount.toString()
        );
      }
    }
    return record;
  }

  private static DeferredDepositNotice noticeFrom(
    DepositRecordEntity record,
    String address
  ) {
    if (record == null || record.getTransactionId() == null) {
      return null;
    }
    return new DeferredDepositNotice(
      record.getUserId(),
      record.getRaw(),
      record.getTxid(),
      address,
      record.getAddressIndex(),
      record.getTransactionId()
    );
  }

  private Optional<DepositAddressEntity> ownerOf(String ticker, String address) {
    if (address == null) {
      return Optional.empty();
    }
    Optional<DepositAddressEntity> owner = depositAddressService.getByAddress(
      ticker,
      address
    );
    return owner == null ? Optional.empty() : owner;
  }

  /**
   * Destinations on an outgoing transfer that land on a Nanobot deposit
   * address, summed per address.
   */
  private List<InternalDeposit> internalDeposits(
    String ticker,
    JSONObject transfer
  ) {
    JSONArray destinations = transfer.optJSONArray("destinations");
    if (destinations == null) {
      return List.of();
    }
    Map<String, InternalDeposit> byAddress = new LinkedHashMap<>();
    for (int i = 0; i < destinations.length(); i++) {
      JSONObject destination = destinations.getJSONObject(i);
      String address = destination.optString(ADDRESS_KEY, null);
      Optional<DepositAddressEntity> owner = ownerOf(ticker, address);
      if (owner.isEmpty()) {
        continue;
      }
      long amount = destination.optLong(AMOUNT_KEY, -1L);
      if (amount <= 0L) {
        continue;
      }
      InternalDeposit next = new InternalDeposit(
        owner.get(),
        BigInteger.valueOf(amount)
      );
      byAddress.merge(
        address,
        next,
        (left, right) ->
          new InternalDeposit(left.owner(), left.amount().add(right.amount()))
      );
    }
    return new ArrayList<>(byAddress.values());
  }

  /**
   * Discovers a self-send at broadcast time, the same moment the outgoing
   * transfer first exists. Uses the wallet's destinations when it has them;
   * otherwise reconstructs them from the batch so a failed lookup still
   * announces the sum.
   */
  private void announceBroadcastDeposits(
    List<QueueEntity> sent,
    CurrencyEntity currencyEntity,
    String txHash,
    List<BigInteger> feeShares,
    Map<String, String> commandMap
  ) {
    JSONObject result = rpc.wallet(
      currencyEntity,
      "get_transfer_by_txid",
      new JSONObject().put("txid", txHash).put(ACCOUNT_INDEX, 0)
    );
    JSONObject transfer = result == null ? null : result.optJSONObject("transfer");
    if (transfer == null || transfer.optJSONArray("destinations") == null) {
      transfer = synthesizeOutgoing(txHash, sent, feeShares);
    } else if (transfer.optString("txid", "").isBlank()) {
      transfer.put("txid", txHash);
    }
    announceInternalDeposits(
      currencyEntity,
      transfer,
      0L,
      ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS),
      commandMap
    );
  }

  private static JSONObject synthesizeOutgoing(
    String txHash,
    List<QueueEntity> sent,
    List<BigInteger> feeShares
  ) {
    JSONArray destinations = new JSONArray();
    for (int i = 0; i < sent.size(); i++) {
      QueueEntity queueEntity = sent.get(i);
      BigInteger amount;
      try {
        amount = new BigInteger(queueEntity.getRaw());
      } catch (NumberFormatException e) {
        continue;
      }
      BigInteger feeShare =
        feeShares != null && i < feeShares.size() ? feeShares.get(i) : null;
      if (feeShare != null) {
        amount = amount.subtract(feeShare);
      }
      if (amount.signum() < 0) {
        amount = BigInteger.ZERO;
      }
      destinations.put(
        new JSONObject()
          .put(ADDRESS_KEY, queueEntity.getTargetAddress())
          .put(AMOUNT_KEY, amount.longValue())
      );
    }
    return new JSONObject().put("txid", txHash).put("destinations", destinations);
  }

  /**
   * Total this address received in {@code transfer}, or null if it is not a
   * destination. Several outputs to the same address are summed so a combined
   * withdrawal to one deposit address is credited in full.
   */
  static BigInteger resolveDestinationAmount(
    JSONObject transfer,
    String address
  ) {
    JSONArray destinations = transfer.optJSONArray("destinations");
    if (destinations == null) {
      return null;
    }
    BigInteger total = null;
    for (int i = 0; i < destinations.length(); i++) {
      JSONObject destination = destinations.getJSONObject(i);
      if (!address.equals(destination.optString(ADDRESS_KEY, null))) {
        continue;
      }
      long amount = destination.optLong(AMOUNT_KEY, -1L);
      if (amount < 0L) {
        continue;
      }
      total = (total == null ? BigInteger.ZERO : total).add(
        BigInteger.valueOf(amount)
      );
    }
    return total;
  }

  /**
   * This output's share of a combined transaction fee, matching
   * {@link #splitFee}. Solo sends still report the full fee.
   */
  static BigInteger destinationFeeShare(
    JSONObject transfer,
    QueueEntity queueEntity
  ) {
    BigInteger fee = broadcastFee(transfer);
    if (fee == null || queueEntity == null) {
      return fee;
    }
    JSONArray destinations = transfer.optJSONArray("destinations");
    if (destinations == null || destinations.length() <= 1) {
      return fee;
    }
    int destCount = destinations.length();
    int index = -1;
    for (int i = 0; i < destCount; i++) {
      JSONObject destination = destinations.getJSONObject(i);
      if (
        !queueEntity
          .getTargetAddress()
          .equals(destination.optString(ADDRESS_KEY, null))
      ) {
        continue;
      }
      BigInteger received = BigInteger.valueOf(
        destination.optLong(AMOUNT_KEY, -1L)
      );
      if (received.signum() < 0) {
        continue;
      }
      if (
        !matchesQueuedAmount(
          new BigInteger(queueEntity.getRaw()),
          received,
          fee
        )
      ) {
        continue;
      }
      index = i;
      break;
    }
    if (index < 0) {
      return fee;
    }
    return splitFee(fee, destCount).get(index);
  }

  // ---------------------------------------------------------------- balances

  private void refreshLiquidity(CurrencyEntity currencyEntity) {
    JSONObject balance = rpc.wallet(
      currencyEntity,
      "get_balance",
      new JSONObject().put(ACCOUNT_INDEX, 0)
    );
    if (balance == null) {
      return;
    }
    // Total balance, not unlocked_balance. Liquidity is what the bot holds, and
    // the audit compares it against user liabilities to check solvency. Monero
    // locks change for 10 blocks after every send, so unlocked_balance drops to
    // zero for minutes at a time and would make a solvent wallet look empty.
    // Nano has no lock concept, so total balance is also the consistent measure
    // across protocols.
    String held = String.valueOf(balance.optLong("balance", 0L));
    if (!held.equals(currencyEntity.getLiquidity())) {
      currencyEntity.setLiquidity(held);
      currenciesService.updateChainState(
        currencyEntity.getId(),
        held,
        null,
        null
      );
    }
  }

}
