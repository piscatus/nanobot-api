package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.DepositAddressEntity;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * transaction pool runs more often so a deposit can be announced to its owner
 * before it is mined, although nothing is credited until it is ten blocks deep.
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
        // Outgoing transfers are scanned too. A send to one of our own deposit
        // addresses is never reported as incoming, so the outgoing side is the
        // only record that the recipient was paid.
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
        creditSelfSend(
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
   * Credits a transfer whose destination is one of our own deposit addresses.
   *
   * <p>Needed because monero-wallet-rpc does not report a send to one of its own
   * subaddresses as an incoming transfer: the funds never "arrive", they are
   * internal to the wallet. Without reading the outgoing side, a user paying
   * another user's shared deposit address, or testing by paying their own, would
   * be debited and nobody would be credited.
   *
   * <p>The destination amount is already net of the network fee, because
   * withdrawals are built with subtract_fee_from_outputs.
   */
  private void creditSelfSend(
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
    if (transfer.optLong("confirmations", 0L) < confirmations) {
      return;
    }

    JSONArray destinations = transfer.optJSONArray("destinations");
    if (destinations == null) {
      return;
    }

    String height = String.valueOf(transfer.optLong("height", 0L));

    for (int i = 0; i < destinations.length(); i++) {
      JSONObject destination = destinations.getJSONObject(i);
      String address = destination.optString(ADDRESS_KEY, null);
      if (address == null) {
        continue;
      }

      // Anything we did not issue is an ordinary external withdrawal.
      Optional<DepositAddressEntity> owner = depositAddressService.getByAddress(
        ticker,
        address
      );
      if (owner.isEmpty()) {
        continue;
      }

      BigInteger amount = BigInteger.valueOf(
        destination.optLong(AMOUNT_KEY, 0L)
      );
      if (amount.signum() <= 0) {
        continue;
      }

      creditToOwner(
        currencyEntity,
        owner.get(),
        txid,
        amount,
        height,
        commandMap
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
    String ticker = currencyEntity.getTicker();
    Long addressIndex = owner.getAddressIndex();
    String userId = owner.getUserId();

    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        txid,
        addressIndex
      )
    ) {
      return;
    }

    DepositRecordEntity record = new DepositRecordEntity(
      ticker,
      txid,
      addressIndex,
      userId,
      amount.toString(),
      height
    );
    record.setId(new ObjectId().toHexString());
    try {
      depositRecordsRepository.insert(record);
    } catch (DuplicateKeyException e) {
      return;
    }

    String transactionId = chainLedgerService.creditDeposit(
      currencyEntity,
      userId,
      amount.toString(),
      txid,
      owner.getAddress(),
      commandMap
    );

    record.setTransactionId(transactionId);
    depositRecordsRepository.save(record);

    fileLogger.info(
      "Credited " +
      amount +
      " " +
      ticker +
      " to user " +
      userId +
      " from " +
      txid
    );
  }

  // ------------------------------------------------------------- withdrawals

  private void processWithdrawals(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap,
    long daemonHeight
  ) {
    String ticker = currencyEntity.getTicker();
    for (QueueEntity queueEntity : queuesService.getQueues()) {
      if (!ticker.equals(queueEntity.getTicker())) {
        continue;
      }
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
        confirmSend(queueEntity, currencyEntity, commandMap);
      } else {
        processSend(queueEntity, currencyEntity, daemonHeight, commandMap);
      }
    }
  }

  private void processSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long daemonHeight,
    Map<String, String> commandMap
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
        // The wallet did not answer. Resending now could double-pay, so wait.
        fileLogger.warn(
          "Cannot verify whether queue #" +
          queueEntity.getId() +
          " was already broadcast; deferring rather than risking a double send."
        );
        return;
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
        return;
      }
    }

    // Mark the attempt height first so the recovery scan above has a lower
    // bound if this process dies mid-send.
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

    JSONObject destination = new JSONObject()
      .put(AMOUNT_KEY, new BigInteger(queueEntity.getRaw()).longValueExact())
      .put(ADDRESS_KEY, queueEntity.getTargetAddress());

    JSONObject result = rpc.wallet(
      currencyEntity,
      "transfer",
      new JSONObject()
        .put("destinations", new JSONArray().put(destination))
        .put(ACCOUNT_INDEX, 0)
        .put("priority", resolveFeePriority(currencyEntity))
        .put("ring_size", 16)
        // The user is debited the full amount and the network fee is taken out
        // of what they receive. That keeps the ledger exactly balanced: the hot
        // wallet drops by precisely the amount burned from the user's balance.
        .put("subtract_fee_from_outputs", new JSONArray().put(0))
        .put("get_tx_key", true)
    );

    if (result == null || !result.has(TX_HASH_KEY)) {
      handleSendFailure(queueEntity, currencyEntity, commandMap);
      return;
    }

    queueEntity.setBlockHash(result.getString(TX_HASH_KEY));
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
      " (fee " +
      result.optLong("fee", 0L) +
      " deducted from the amount sent)"
    );

    // Only on a fresh broadcast. The recovery path above cannot tell whether
    // this was already announced before the crash, and a second "sent" would
    // look like a second withdrawal.
    chainLedgerService.notifyWithdrawalSent(
      currencyEntity,
      queueEntity,
      ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS),
      commandMap
    );
  }

  /**
   * Counts a failed send attempt and, once attempts are exhausted, refunds the
   * user rather than retrying forever.
   *
   * <p>The common permanent failure is a fee larger than the amount being sent:
   * with the fee deducted from the output there is nothing left to pay the
   * recipient, and that fails identically on every retry. Since the balance was
   * already debited when the withdrawal was queued, leaving it queued would
   * strand the user's funds indefinitely.
   */
  private void handleSendFailure(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
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
      " (attempt " +
      attempts +
      " of " +
      MAX_SEND_ATTEMPTS +
      ")"
    );

    if (attempts < MAX_SEND_ATTEMPTS) {
      return;
    }

    // Never refund without proving nothing reached the network, or the user
    // would be credited for a withdrawal they actually received.
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
        " has exhausted its attempts, but the wallet cannot confirm whether it " +
        "was broadcast. Holding for manual review rather than refunding blind."
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
      queueEntity.setBlockHash(check.txid());
      queueEntity.setProcessed(true);
      queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));
      return;
    }

    String minimum = currenciesService.getCurrencyDecimalValue(
      currenciesService.getEffectiveMinimumWithdraw(
        new CurrencyDto(currencyEntity)
      ),
      Integer.parseInt(currencyEntity.getPrecision())
    );

    String reason =
      "## Network fees exceeded the amount requested, so the transaction could " +
      "not be created. Your funds were **not** sent and have been returned to " +
      "your balance.\n" +
      "-# The current minimum withdrawal, including network fees, is **" +
      minimum +
      " " +
      currencyEntity.getTicker() +
      "**.";

    String refundTransactionId = chainLedgerService.refundFailedWithdrawal(
      currencyEntity,
      queueEntity,
      reason,
      commandMap
    );

    // Only drop the queue entry once the refund is booked. If it failed the
    // entry stays so the discrepancy stays visible instead of vanishing.
    if (refundTransactionId != null) {
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
   * <p>Net is the real case, because the fee is taken out of the output. Gross
   * is accepted too so the check does not depend on that staying true.
   */
  static boolean matchesQueuedAmount(
    BigInteger expected,
    BigInteger received,
    BigInteger fee
  ) {
    return (
      expected.compareTo(received) == 0 ||
      expected.compareTo(received.add(fee)) == 0
    );
  }

  private void confirmSend(
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
      return;
    }
    JSONObject transfer = result.optJSONObject("transfer");
    if (transfer == null) {
      return;
    }
    if (
      transfer.optLong("confirmations", 0L) <
      ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS)
    ) {
      return;
    }
    // Wait for the destination to be spendable before crediting the recipient,
    // matching the rule applied to ordinary deposits.

    // A withdrawal to one of our own deposit addresses has to be credited from
    // the outgoing transfer. The wallet does not report a send to its own
    // subaddress as an incoming transfer, so the deposit scanner never sees it,
    // and without this the recipient would be debited-but-never-credited.
    //
    // The ledger credit happens here, before the queue entry is dropped, since
    // that order is what keeps a crash in between recoverable. Only the user's
    // deposit notice is held back, so the withdrawal that caused it is
    // announced first and the two messages read in the order things happened.
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
        commandMap
      );
    }

    if (deferred != null) {
      chainLedgerService.notifyDepositConfirmed(
        currencyEntity,
        deferred.userId(),
        deferred.raw(),
        deferred.txid(),
        deferred.address(),
        deferred.transactionId(),
        commandMap
      );
    }
  }

  /**
   * A deposit that has been credited on the ledger but whose user notice has
   * been held back, so the caller can send it after the withdrawal notice.
   */
  private record DeferredDepositNotice(
    String userId,
    String raw,
    String txid,
    String address,
    String transactionId
  ) {}

  /**
   * Credits the owner of a deposit address that received one of our own
   * withdrawals, and returns the notice still owed to them, or null when nothing
   * was credited.
   *
   * <p>Credits what the destination actually received, which is the requested
   * amount minus the network fee, taken from the transfer's own destination
   * entry rather than recomputed. Guarded by the same depositRecords unique
   * index as a normal deposit, so it cannot double-credit.
   */
  private DeferredDepositNotice creditInternalDestination(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    JSONObject transfer,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();
    Optional<DepositAddressEntity> recipient =
      depositAddressService.getByAddress(
        ticker,
        queueEntity.getTargetAddress()
      );
    if (recipient.isEmpty()) {
      return null;
    }

    String txid = queueEntity.getBlockHash();
    Long addressIndex = recipient.get().getAddressIndex();

    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        txid,
        addressIndex
      )
    ) {
      return null;
    }

    BigInteger received = resolveDestinationAmount(
      transfer,
      queueEntity.getTargetAddress()
    );
    if (received == null || received.signum() <= 0) {
      fileLogger.error(
        "Could not determine the amount received by " +
        queueEntity.getTargetAddress() +
        " in " +
        txid +
        "; the recipient has not been credited."
      );
      return null;
    }

    String userId = recipient.get().getUserId();
    DepositRecordEntity record = new DepositRecordEntity(
      ticker,
      txid,
      addressIndex,
      userId,
      received.toString(),
      String.valueOf(transfer.optLong("height", 0L))
    );
    record.setId(new ObjectId().toHexString());
    try {
      depositRecordsRepository.insert(record);
    } catch (DuplicateKeyException e) {
      return null;
    }

    String transactionId = chainLedgerService.creditDeposit(
      currencyEntity,
      userId,
      received.toString(),
      txid,
      recipient.get().getAddress(),
      commandMap,
      false
    );
    record.setTransactionId(transactionId);
    depositRecordsRepository.save(record);

    fileLogger.info(
      "Credited " +
      received +
      " " +
      ticker +
      " to user " +
      userId +
      " from internal withdrawal " +
      txid
    );

    if (transactionId == null) {
      // The transfer failed and was logged by the ledger service. There is
      // nothing to announce, and announcing would claim a credit that did not
      // happen.
      return null;
    }

    return new DeferredDepositNotice(
      userId,
      received.toString(),
      txid,
      recipient.get().getAddress(),
      transactionId
    );
  }

  /** The amount a specific address received in a transfer, or null if absent. */
  private BigInteger resolveDestinationAmount(
    JSONObject transfer,
    String address
  ) {
    JSONArray destinations = transfer.optJSONArray("destinations");
    if (destinations == null) {
      return null;
    }
    for (int i = 0; i < destinations.length(); i++) {
      JSONObject destination = destinations.getJSONObject(i);
      if (address.equals(destination.optString(ADDRESS_KEY, null))) {
        return BigInteger.valueOf(destination.optLong(AMOUNT_KEY, 0L));
      }
    }
    return null;
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
