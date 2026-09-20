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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Bitcoin integration.
 *
 * <p>Closer to Monero than to Nano: one hot wallet issues an address per user,
 * so deposits arrive already inside the wallet and there is no per-deposit
 * sweep to pay for. The only fee is on withdrawal.
 *
 * <p>Deposit detection is a {@code listsinceblock} walk from just below the
 * cursor. It runs every pass: crediting only happens as the chain advances,
 * which on Bitcoin means roughly every ten minutes, but a deposit sitting in
 * the mempool is announced to its owner as soon as the wallet sees it. No
 * websocket is involved; bitcoind hands back a confirmation count per entry, so
 * maturity is a comparison rather than something to track.
 *
 * <p>The node carries no txindex, so every lookup here is deliberately
 * wallet-scoped. {@code getrawtransaction} against an arbitrary hash would fail
 * on this node.
 */
@Service
public class BitcoinChainAdapter implements ChainAdapter {

  /**
   * Six blocks is the conventional settlement point and the depth at which a
   * reorg stops being a practical concern.
   */
  private static final int DEFAULT_CONFIRMATIONS = 6;

  /**
   * Re-scan this far below the cursor each pass so a shallow reorg cannot drop a
   * deposit. Safe to repeat because depositRecords has a unique index that
   * rejects a second credit for the same output.
   */
  private static final long REORG_LOOKBACK = 6L;

  /**
   * Virtual size in vbytes used to turn a fee rate into an absolute estimate. A
   * one-input two-output P2WPKH spend is about 141 vbytes and each extra input
   * adds roughly 68, so this sits generously above the common case: an estimate
   * that is too low is the failure this exists to prevent.
   */
  private static final long ESTIMATED_TX_VSIZE = 250L;

  /**
   * Attempts before a withdrawal is treated as permanently failed. A send that
   * cannot be built - typically because the fee leaves nothing above the dust
   * threshold - fails identically every time, so retrying forever would strand
   * the user's already debited balance.
   */
  private static final int MAX_SEND_ATTEMPTS = 3;

  /**
   * How often to re-scan the wallet's entire history instead of just the window
   * around the cursor. The windowed scan only looks a short way back, so a
   * deposit maturing while the API was down for longer than that would fall out
   * of range permanently. Cheap to do rarely because crediting is idempotent.
   */
  private static final long FULL_SCAN_INTERVAL_MS = 10L * 60L * 1000L;

  /**
   * Fee refresh interval. Unlike Monero's single call this needs three RPCs, and
   * fee rates move on mempool timescales rather than per second, so running it
   * on every pass would be pure noise against the node.
   */
  private static final long FEE_REFRESH_INTERVAL_MS = 60L * 1000L;

  private static final int DEFAULT_FEE_PRIORITY = 2;

  /**
   * Blocks a broadcast withdrawal may go unconfirmed before it is worth
   * complaining about. Roughly a day, which is far beyond the configured
   * confirmation target and so only reachable if the fee was left behind by a
   * rising market.
   */
  private static final long STUCK_WITHDRAWAL_BLOCKS = 144L;

  /**
   * Confirmation targets for priority 1 through 4. Reuses the existing Monero
   * shaped feePriority field rather than adding a Bitcoin-only column.
   */
  private static final int[] CONFIRMATION_TARGETS = { 25, 6, 3, 1 };

  private static final String ESTIMATE_MODE = "economical";
  private static final String ADDRESS_TYPE = "bech32";

  private static final BigDecimal SATOSHIS_PER_BTC = BigDecimal.valueOf(
    100_000_000L
  );

  private static final String ADDRESS_KEY = "address";
  private static final String AMOUNT_KEY = "amount";
  private static final String TXID_KEY = "txid";
  private static final String CATEGORY_KEY = "category";
  private static final String CONFIRMATIONS_KEY = "confirmations";
  private static final String FEERATE_KEY = "feerate";

  /** Deliberately not persisted: a restart forces a full sweep, which is what is wanted after downtime. */
  private volatile long lastFullScanMillis = 0L;

  /**
   * How often to scan when no block has arrived, purely to spot deposits that
   * are still in the mempool. Ten seconds keeps the wallet from being polled
   * every pass while still telling a user about their deposit almost as soon as
   * it is broadcast.
   */
  private static final long DISCOVERY_SCAN_INTERVAL_MS = 10L * 1000L;

  private volatile long lastDiscoveryScanMillis = 0L;

  private volatile long lastFeeRefreshMillis = 0L;

  private final BitcoinRpcClient rpc;
  private final CurrenciesService currenciesService;
  private final DepositAddressService depositAddressService;
  private final DepositRecordsRepository depositRecordsRepository;
  private final DepositNoticeService depositNoticeService;
  private final QueuesService queuesService;
  private final FileLogger fileLogger;

  /**
   * Field injected because ChainLedgerServiceImpl reaches adapters through the
   * registry, so a constructor dependency here would close a cycle Spring
   * rejects.
   */
  @Autowired
  private ChainLedgerService chainLedgerService;

  public BitcoinChainAdapter(
    BitcoinRpcClient rpc,
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
    this.fileLogger = new FileLogger("BitcoinChainAdapter");
  }

  @Override
  public String protocol() {
    return Constants.PROTOCOL_BITCOIN;
  }

  @Override
  public boolean supportsRepresentative() {
    return false;
  }

  // ------------------------------------------------------- deposit addresses

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

    JSONObject created = rpc.wallet(
      currencyEntity,
      "getnewaddress",
      "user:" + userId,
      ADDRESS_TYPE
    );
    String address = created == null
      ? null
      : created.optString(BitcoinRpcClient.RESULT_KEY, null);
    if (address == null || address.isBlank()) {
      fileLogger.error(
        "Could not create a " + ticker + " address for user " + userId
      );
      return null;
    }

    JSONObject info = rpc.wallet(currencyEntity, "getaddressinfo", address);
    Long addressIndex = info == null
      ? null
      : parseDerivationIndex(info.optString("hdkeypath", null));
    if (addressIndex == null) {
      // Storing the address without its index would break the unique key that
      // deposit records are booked against, so refuse rather than guess one.
      fileLogger.error(
        "Could not read a derivation index for " +
        ticker +
        " address " +
        address +
        "; it has not been assigned to user " +
        userId
      );
      return null;
    }

    return depositAddressService
      .record(userId, ticker, address, addressIndex)
      .getAddress();
  }

  /**
   * The final component of an HD key path, used as the address index.
   *
   * <p>bitcoind allocates a fresh one per getnewaddress, which makes it unique
   * without a counter of our own and the race that would come with it. Bitcoin
   * Core writes hardened levels as {@code 0h} in current builds and {@code 0'}
   * in older ones; the receiving level is never hardened, but both are accepted
   * so a node upgrade cannot quietly change the answer.
   */
  static Long parseDerivationIndex(String hdKeyPath) {
    if (hdKeyPath == null || hdKeyPath.isBlank()) {
      return null;
    }
    int lastSlash = hdKeyPath.lastIndexOf('/');
    if (lastSlash < 0 || lastSlash == hdKeyPath.length() - 1) {
      return null;
    }
    String last = hdKeyPath.substring(lastSlash + 1).trim();
    if (last.endsWith("h") || last.endsWith("H") || last.endsWith("'")) {
      last = last.substring(0, last.length() - 1);
    }
    try {
      long index = Long.parseLong(last);
      return index < 0L ? null : index;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  // ---------------------------------------------------------------- activity

  @Override
  public void processActivity(
    UserDetailsEntity botUserDetails,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    Long chainHeight = fetchChainHeight(currencyEntity);
    if (chainHeight == null) {
      return;
    }

    if (currencyEntity.getProcessWithdrawals()) {
      processWithdrawals(currencyEntity, commandMap, chainHeight);
    }

    if (currencyEntity.getProcessDeposits()) {
      processDeposits(currencyEntity, chainHeight, commandMap);
    }

    refreshFeeEstimate(currencyEntity);
    refreshLiquidity(currencyEntity);
  }

  /**
   * getblockchaininfo is cheap, so it gates the more expensive wallet work.
   * Returns null when the node is unreachable or still syncing, skipping the
   * pass.
   */
  private Long fetchChainHeight(CurrencyEntity currencyEntity) {
    JSONObject info = rpc.daemon(currencyEntity, "getblockchaininfo");
    if (info == null) {
      fileLogger.warn(currencyEntity.getTicker() + " node is unreachable");
      return null;
    }
    if (info.optBoolean("initialblockdownload", false)) {
      fileLogger.warn(
        currencyEntity.getTicker() + " node is still downloading blocks"
      );
      return null;
    }
    return info.optLong("blocks", 0L);
  }

  // ---------------------------------------------------------------- deposits

  private void processDeposits(
    CurrencyEntity currencyEntity,
    long chainHeight,
    Map<String, String> commandMap
  ) {
    int confirmations = ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS);
    long cursor = ChainSettings.parseLong(currencyEntity.getLastScannedHeight(), 0L);
    long safeHeight = chainHeight - confirmations;

    boolean fullScan =
      System.currentTimeMillis() - lastFullScanMillis >= FULL_SCAN_INTERVAL_MS;

    // Even when nothing can have matured the scan still runs now and then,
    // because listsinceblock is also how a brand new, still unconfirmed deposit
    // is first seen, and the user is told about that when it shows up rather
    // than an hour later when it is credited. Crediting stays gated on the
    // confirmation count regardless.
    long now = System.currentTimeMillis();
    if (
      !fullScan &&
      safeHeight <= cursor &&
      now - lastDiscoveryScanMillis < DISCOVERY_SCAN_INTERVAL_MS
    ) {
      return;
    }
    lastDiscoveryScanMillis = now;

    // A full sweep passes no starting block, which re-offers the wallet's whole
    // history. Anything already credited is rejected by the depositRecords
    // unique index, so this can only fill in gaps.
    Object fromBlock = JSONObject.NULL;
    if (!fullScan) {
      String blockHash = resolveBlockHash(
        currencyEntity,
        Math.max(0L, cursor - REORG_LOOKBACK)
      );
      if (blockHash == null) {
        return;
      }
      fromBlock = blockHash;
    }

    JSONObject result = listSinceBlock(currencyEntity, fromBlock);
    if (result == null) {
      return;
    }
    JSONArray transactions = result.optJSONArray("transactions");
    if (transactions == null) {
      return;
    }

    for (Deposit deposit : groupDeposits(transactions, confirmations).values()) {
      creditDeposit(currencyEntity, deposit, commandMap);
    }

    for (Deposit deposit : groupImmatureDeposits(
      transactions,
      confirmations
    )
      .values()) {
      announceDeposit(currencyEntity, deposit, confirmations, commandMap);
    }

    if (fullScan) {
      lastFullScanMillis = System.currentTimeMillis();
      fileLogger.info(
        "Completed a full " +
        currencyEntity.getTicker() +
        " deposit reconciliation sweep."
      );
    }

    // Advance only to the matured height so an output is never skipped before it
    // has enough confirmations to be credited.
    if (safeHeight > cursor) {
      currenciesService.updateChainState(
        currencyEntity.getId(),
        null,
        null,
        String.valueOf(safeHeight)
      );
    }
  }

  /**
   * One credited deposit: everything a single transaction paid to a single
   * address, summed.
   */
  record Deposit(
    String txid,
    String address,
    BigInteger amount,
    long height,
    long confirmations
  ) {
    Deposit plus(BigInteger more) {
      return new Deposit(txid, address, amount.add(more), height, confirmations);
    }
  }

  /**
   * Collapses wallet entries into one deposit per transaction and address,
   * keeping only those with enough confirmations to be credited.
   *
   * <p>A transaction may legally pay the same address in more than one output,
   * and bitcoind lists each separately. Deposit records are keyed on
   * (ticker, txid, addressIndex), so crediting them one by one would book the
   * first and have the unique index silently swallow the rest, under-crediting
   * the user. Summing first also matches how Monero reports a transfer, which
   * keeps the two adapters behaving the same way.
   */
  static Map<String, Deposit> groupDeposits(
    JSONArray transactions,
    int confirmations
  ) {
    return groupDeposits(transactions, depth -> depth >= confirmations);
  }

  /**
   * The complement of {@link #groupDeposits}: deposits the wallet has seen that
   * are not yet old enough to credit, from the mempool up to one block short.
   *
   * <p>Negative counts are excluded. bitcoind reports those for a transaction
   * that conflicts with one already mined, and it will never be credited, so
   * announcing it would promise funds that are not coming.
   */
  static Map<String, Deposit> groupImmatureDeposits(
    JSONArray transactions,
    int confirmations
  ) {
    return groupDeposits(
      transactions,
      depth -> depth >= 0L && depth < confirmations
    );
  }

  private static Map<String, Deposit> groupDeposits(
    JSONArray transactions,
    java.util.function.LongPredicate depthAccepted
  ) {
    Map<String, Deposit> deposits = new LinkedHashMap<>();
    for (int i = 0; i < transactions.length(); i++) {
      JSONObject entry = transactions.optJSONObject(i);
      if (entry == null) {
        continue;
      }
      if (!"receive".equals(entry.optString(CATEGORY_KEY, null))) {
        continue;
      }
      long depth = entry.optLong(CONFIRMATIONS_KEY, 0L);
      if (!depthAccepted.test(depth)) {
        continue;
      }
      String txid = entry.optString(TXID_KEY, null);
      String address = entry.optString(ADDRESS_KEY, null);
      if (txid == null || address == null) {
        continue;
      }
      BigInteger amount = toSatoshis(entry.opt(AMOUNT_KEY));
      if (amount == null || amount.signum() <= 0) {
        continue;
      }
      Deposit deposit = new Deposit(
        txid,
        address,
        amount,
        entry.optLong("blockheight", 0L),
        depth
      );
      deposits.merge(txid + "|" + address, deposit, (a, b) -> a.plus(b.amount())
      );
    }
    return deposits;
  }

  /**
   * Tells the address owner about a deposit that has been seen but not yet
   * credited, once.
   *
   * <p>Skipped when a deposit record already exists: that means it was credited
   * on an earlier pass and this is a re-scan seeing a stale confirmation count,
   * and a "discovered" notice after a "confirmed" one would read as a second
   * deposit. The notice store then makes the announcement itself exactly-once.
   */
  private void announceDeposit(
    CurrencyEntity currencyEntity,
    Deposit deposit,
    int confirmations,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();

    Optional<DepositAddressEntity> owner = depositAddressService.getByAddress(
      ticker,
      deposit.address()
    );
    if (owner.isEmpty()) {
      return;
    }

    Long addressIndex = owner.get().getAddressIndex();
    String userId = owner.get().getUserId();

    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        deposit.txid(),
        addressIndex
      )
    ) {
      return;
    }

    if (
      !depositNoticeService.recordFirstSighting(
        ticker,
        deposit.txid(),
        addressIndex,
        userId,
        deposit.amount().toString()
      )
    ) {
      return;
    }

    chainLedgerService.notifyDepositDiscovered(
      currencyEntity,
      userId,
      deposit.amount().toString(),
      deposit.txid(),
      deposit.address(),
      deposit.confirmations(),
      confirmations,
      commandMap
    );

    fileLogger.info(
      "Discovered " +
      deposit.amount() +
      " " +
      ticker +
      " for user " +
      userId +
      " in " +
      deposit.txid() +
      " (" +
      deposit.confirmations() +
      " of " +
      confirmations +
      " confirmations)"
    );
  }

  /**
   * Books a deposit to the address owner exactly once.
   *
   * <p>The depositRecords row is inserted before the credit so its unique index
   * is what enforces exactly-once. A concurrent or repeated scan fails the
   * insert and returns instead of crediting again, which is what makes the
   * periodic full sweep safe to run.
   */
  private void creditDeposit(
    CurrencyEntity currencyEntity,
    Deposit deposit,
    Map<String, String> commandMap
  ) {
    String ticker = currencyEntity.getTicker();

    Optional<DepositAddressEntity> owner = depositAddressService.getByAddress(
      ticker,
      deposit.address()
    );
    if (owner.isEmpty()) {
      // Not an address we issued to anyone, so this is house money - the hot
      // wallet being funded, or change. Nothing to credit, and nothing wrong.
      return;
    }

    Long addressIndex = owner.get().getAddressIndex();
    String userId = owner.get().getUserId();

    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        deposit.txid(),
        addressIndex
      )
    ) {
      return;
    }

    DepositRecordEntity record = new DepositRecordEntity(
      ticker,
      deposit.txid(),
      addressIndex,
      userId,
      deposit.amount().toString(),
      String.valueOf(deposit.height())
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
      deposit.amount().toString(),
      deposit.txid(),
      deposit.address(),
      commandMap
    );

    record.setTransactionId(transactionId);
    depositRecordsRepository.save(record);

    fileLogger.info(
      "Credited " +
      deposit.amount() +
      " " +
      ticker +
      " to user " +
      userId +
      " from " +
      deposit.txid()
    );
  }

  // ------------------------------------------------------------- withdrawals

  private void processWithdrawals(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap,
    long chainHeight
  ) {
    String ticker = currencyEntity.getTicker();
    for (QueueEntity queueEntity : queuesService.getQueuesByTicker(ticker)) {
      if (queueEntity.getLevel() == LevelDto.UPDATE) {
        // Bitcoin has no representative to set. Such an entry can only be a
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
        confirmSend(queueEntity, currencyEntity, chainHeight, commandMap);
      } else {
        processSend(queueEntity, currencyEntity, chainHeight, commandMap);
      }
    }
  }

  private void processSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long chainHeight,
    Map<String, String> commandMap
  ) {
    // Recover before sending again. If a previous attempt broadcast but died
    // before the hash was saved, sending now would pay the user twice.
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
        return;
      }
      if (check.txid() != null) {
        fileLogger.warn(
          "Recovered an in-flight " +
          currencyEntity.getTicker() +
          " withdrawal instead of resending: " +
          check.txid()
        );
        queueEntity.setBlockHash(check.txid());
        queueEntity.setProcessed(true);
        queuesService.updateQueueProgress(
          queueEntity.getId(),
          null,
          null,
          check.txid(),
          true
        );
        return;
      }
    }

    // The regex on the currency document cannot check a checksum, so a single
    // mistyped character reaches this point looking valid. Asking the node
    // settles it before anything is spent, and turns three doomed attempts into
    // one accurate refund.
    if (!isValidAddress(currencyEntity, queueEntity.getTargetAddress())) {
      fileLogger.error(
        "Refusing to send queue #" +
        queueEntity.getId() +
        ": the node rejected destination " +
        queueEntity.getTargetAddress()
      );
      refund(
        queueEntity,
        currencyEntity,
        "## That address was rejected by the network, so nothing was sent.\n" +
        "-# Your funds have been returned to your balance. Please check the " +
        "address and try again.",
        commandMap
      );
      return;
    }

    // Mark the attempt height first so the recovery scan above has a lower bound
    // if this process dies mid-send.
    if (queueEntity.getIndex() == null) {
      queueEntity.setIndex(chainHeight);
      queuesService.updateQueueProgress(
        queueEntity.getId(),
        null,
        chainHeight,
        null,
        null
      );
    }

    JSONObject result = rpc.wallet(
      currencyEntity,
      "sendtoaddress",
      queueEntity.getTargetAddress(),
      toBitcoin(new BigInteger(queueEntity.getRaw())),
      broadcastMarker(queueEntity),
      "",
      // The user is debited the full amount and the network fee comes out of
      // what they receive, so the hot wallet drops by precisely the amount
      // burned from the user's balance and the ledger stays balanced.
      true,
      // Never signal RBF. A replaceable withdrawal could be swapped out after
      // its txid was recorded as final.
      false,
      resolveConfirmationTarget(currencyEntity),
      ESTIMATE_MODE
    );

    String txid = result == null
      ? null
      : result.optString(BitcoinRpcClient.RESULT_KEY, null);
    if (txid == null || txid.isBlank()) {
      handleSendFailure(queueEntity, currencyEntity, commandMap);
      return;
    }

    queueEntity.setBlockHash(txid);
    queueEntity.setProcessed(true);
    queuesService.updateQueueProgress(
      queueEntity.getId(),
      null,
      null,
      txid,
      true
    );

    fileLogger.info(
      "Submitted " +
      currencyEntity.getTicker() +
      " withdrawal " +
      txid +
      " for queue #" +
      queueEntity.getId()
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

  private void confirmSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long chainHeight,
    Map<String, String> commandMap
  ) {
    JSONObject transaction = rpc.wallet(
      currencyEntity,
      "gettransaction",
      queueEntity.getBlockHash()
    );
    if (transaction == null) {
      return;
    }

    long confirmations = transaction.optLong(CONFIRMATIONS_KEY, 0L);

    warnIfStuck(queueEntity, chainHeight, confirmations);

    // A negative count means the transaction conflicts with one that confirmed,
    // so it will never settle. Nano and Monero have no equivalent state. It is
    // left queued and logged loudly rather than refunded automatically: with
    // replaceable off and a single wallet spending these inputs, getting here at
    // all means something unusual happened, and a wrong guess pays twice.
    if (confirmations < 0L || transaction.optBoolean("abandoned", false)) {
      fileLogger.error(
        "Withdrawal " +
        queueEntity.getBlockHash() +
        " for queue #" +
        queueEntity.getId() +
        " is conflicted or abandoned (" +
        confirmations +
        " confirmations). Holding for manual review."
      );
      return;
    }

    if (confirmations < ChainSettings.confirmations(currencyEntity, DEFAULT_CONFIRMATIONS)) {
      return;
    }

    // A withdrawal aimed at one of our own deposit addresses needs no special
    // handling: bitcoind reports an internal transfer as both a send and a
    // receive, so the ordinary deposit scan credits the recipient. Monero needed
    // a dedicated path here only because its wallet never reports such a
    // transfer as incoming.

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
  }

  /**
   * Flags a broadcast withdrawal that is taking far too long to confirm.
   *
   * <p>Nothing here can fix it. The transaction is final from our side - it is
   * not replaceable, so the fee cannot be bumped - and refunding would be
   * unsafe while it can still be mined. But without this it fails completely
   * silently: the entry simply waits, the user is never notified, and no error
   * is ever logged. A fee spike immediately after broadcast is the realistic
   * way to get here, and it needs a human either to wait it out or to bump the
   * transaction by hand.
   *
   * <p>Deliberately derived from the recorded attempt height rather than a new
   * timestamp field, so it needs no schema change and survives a restart.
   */
  private void warnIfStuck(
    QueueEntity queueEntity,
    long chainHeight,
    long confirmations
  ) {
    if (confirmations > 0L || queueEntity.getIndex() == null) {
      return;
    }
    long waited = chainHeight - queueEntity.getIndex();
    // Only on the exact block so this logs once a day rather than once a second.
    if (waited > 0 && waited % STUCK_WITHDRAWAL_BLOCKS == 0) {
      fileLogger.error(
        "Withdrawal " +
        queueEntity.getBlockHash() +
        " for queue #" +
        queueEntity.getId() +
        " is still unconfirmed after " +
        waited +
        " blocks. It cannot be replaced or refunded safely; it needs a manual " +
        "fee bump or a decision to keep waiting."
      );
    }
  }

  /**
   * Counts a failed send attempt and, once attempts are exhausted, refunds the
   * user rather than retrying forever.
   *
   * <p>The common permanent failure is an amount that cannot survive the fee:
   * with the fee deducted from the output, what is left falls under the dust
   * threshold and the node refuses to build the transaction. That fails
   * identically on every retry, and the balance was debited when the withdrawal
   * was queued, so leaving it here would strand the user's funds.
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
      queueEntity.getIndex() == null ? 0L : queueEntity.getIndex()
    );

    if (!check.conclusive()) {
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

    refund(
      queueEntity,
      currencyEntity,
      "## Network fees exceeded the amount requested, so the transaction could " +
      "not be created. Your funds were **not** sent and have been returned to " +
      "your balance.\n" +
      "-# The current minimum withdrawal, including network fees, is **" +
      minimum +
      " " +
      currencyEntity.getTicker() +
      "**.",
      commandMap
    );
  }

  /**
   * Credits a withdrawal back and drops the queue entry, but only once the
   * refund is booked. If it failed the entry stays so the discrepancy stays
   * visible instead of vanishing.
   */
  private void refund(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    String reason,
    Map<String, String> commandMap
  ) {
    String refundTransactionId = chainLedgerService.refundFailedWithdrawal(
      currencyEntity,
      queueEntity,
      reason,
      commandMap
    );
    if (refundTransactionId != null) {
      queuesService.deleteQueue(queueEntity.getId());
    }
  }

  /**
   * Looks for an outgoing transaction belonging to this queue entry, used to
   * tell "the send never happened" apart from "the send happened but was not
   * recorded".
   *
   * <p>Matched on the comment stamped at send time rather than on address and
   * amount. Amount matching would be wrong here anyway, because the fee is
   * subtracted from the output and so the wallet reports less than was
   * requested, and two withdrawals to the same address for the same amount are
   * perfectly legal. The queue id is exact.
   */
  private BroadcastCheck findAlreadySentTx(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    long fromHeight
  ) {
    String blockHash = resolveBlockHash(
      currencyEntity,
      Math.max(0L, fromHeight - REORG_LOOKBACK)
    );
    if (blockHash == null) {
      return BroadcastCheck.unknown();
    }

    JSONObject result = listSinceBlock(currencyEntity, blockHash);
    if (result == null) {
      return BroadcastCheck.unknown();
    }
    JSONArray transactions = result.optJSONArray("transactions");
    if (transactions == null) {
      return BroadcastCheck.unknown();
    }

    String marker = broadcastMarker(queueEntity);
    for (int i = 0; i < transactions.length(); i++) {
      JSONObject entry = transactions.optJSONObject(i);
      if (entry == null) {
        continue;
      }
      if (!"send".equals(entry.optString(CATEGORY_KEY, null))) {
        continue;
      }
      if (marker.equals(entry.optString("comment", null))) {
        return BroadcastCheck.sent(entry.optString(TXID_KEY, null));
      }
    }
    return BroadcastCheck.notSent();
  }

  /** Stamped into the wallet comment so a broadcast can be traced back to its queue entry. */
  private static String broadcastMarker(QueueEntity queueEntity) {
    return "nanobot:" + queueEntity.getId();
  }

  // ------------------------------------------------------------ fee and size

  /**
   * Refreshes the stored network fee estimate. Kept as its own field rather than
   * folded into minimumWithdraw, because adding it on each refresh would
   * compound and drive the minimum up without bound.
   */
  private void refreshFeeEstimate(CurrencyEntity currencyEntity) {
    if (
      System.currentTimeMillis() - lastFeeRefreshMillis < FEE_REFRESH_INTERVAL_MS
    ) {
      return;
    }
    lastFeeRefreshMillis = System.currentTimeMillis();

    JSONObject estimate = rpc.daemon(
      currencyEntity,
      "estimatesmartfee",
      resolveConfirmationTarget(currencyEntity),
      ESTIMATE_MODE
    );

    BigDecimal perKvb = estimate == null
      ? null
      : toBigDecimal(estimate.opt(FEERATE_KEY));

    // estimatesmartfee can return nothing but an errors array when it has too
    // little data. The relay floors below still give a usable number, so this
    // carries on rather than skipping the refresh.
    BigDecimal floor = resolveRelayFloor(currencyEntity);
    if (perKvb == null || perKvb.signum() <= 0) {
      perKvb = floor;
    } else if (floor != null && floor.compareTo(perKvb) > 0) {
      // A fee under the relay minimum builds a transaction the network will not
      // forward, so the floor wins.
      perKvb = floor;
    }
    if (perKvb == null || perKvb.signum() <= 0) {
      return;
    }

    String fee = perKvb
      .multiply(SATOSHIS_PER_BTC)
      .multiply(BigDecimal.valueOf(ESTIMATED_TX_VSIZE))
      .divide(BigDecimal.valueOf(1000L), 0, RoundingMode.CEILING)
      .toBigInteger()
      .toString();

    if (!fee.equals(currencyEntity.getFeeEstimate())) {
      currencyEntity.setFeeEstimate(fee);
      currenciesService.updateChainState(
        currencyEntity.getId(),
        null,
        fee,
        null
      );
    }
  }

  /** The higher of the node's relay minimum and the current mempool minimum, in BTC per kvB. */
  private BigDecimal resolveRelayFloor(CurrencyEntity currencyEntity) {
    BigDecimal floor = null;

    JSONObject network = rpc.daemon(currencyEntity, "getnetworkinfo");
    if (network != null) {
      floor = toBigDecimal(network.opt("relayfee"));
    }

    JSONObject mempool = rpc.daemon(currencyEntity, "getmempoolinfo");
    if (mempool != null) {
      BigDecimal mempoolMinimum = toBigDecimal(mempool.opt("mempoolminfee"));
      if (
        mempoolMinimum != null &&
        (floor == null || mempoolMinimum.compareTo(floor) > 0)
      ) {
        floor = mempoolMinimum;
      }
    }

    return floor;
  }

  private int resolveConfirmationTarget(CurrencyEntity currencyEntity) {
    long configured = ChainSettings.parseLong(
      currencyEntity.getFeePriority(),
      DEFAULT_FEE_PRIORITY
    );
    if (configured < 1L || configured > CONFIRMATION_TARGETS.length) {
      configured = DEFAULT_FEE_PRIORITY;
    }
    return CONFIRMATION_TARGETS[(int) configured - 1];
  }

  // ---------------------------------------------------------------- balances

  private void refreshLiquidity(CurrencyEntity currencyEntity) {
    JSONObject balances = rpc.wallet(currencyEntity, "getbalances");
    if (balances == null) {
      return;
    }
    JSONObject mine = balances.optJSONObject("mine");
    if (mine == null) {
      return;
    }

    // Everything the wallet holds, not just what is spendable right now. The
    // audit compares liquidity against user liabilities to check solvency, and
    // change pending its first confirmation is still the bot's money. This also
    // matches how the Monero and Nano adapters report it.
    BigInteger held = BigInteger.ZERO;
    for (String bucket : new String[] {
      "trusted",
      "untrusted_pending",
      "immature",
    }) {
      BigInteger value = toSatoshis(mine.opt(bucket));
      if (value != null) {
        held = held.add(value);
      }
    }

    String liquidity = held.toString();
    if (!liquidity.equals(currencyEntity.getLiquidity())) {
      currencyEntity.setLiquidity(liquidity);
      currenciesService.updateChainState(
        currencyEntity.getId(),
        liquidity,
        null,
        null
      );
    }
  }

  // ----------------------------------------------------------------- helpers

  private boolean isValidAddress(
    CurrencyEntity currencyEntity,
    String address
  ) {
    JSONObject result = rpc.daemon(currencyEntity, "validateaddress", address);
    // A null result is a node problem rather than a verdict on the address.
    // Treating that as invalid would refund a perfectly good withdrawal, so an
    // unreachable node lets it through to the send, which fails safely.
    return result == null || result.optBoolean("isvalid", false);
  }

  private String resolveBlockHash(
    CurrencyEntity currencyEntity,
    long height
  ) {
    JSONObject result = rpc.daemon(currencyEntity, "getblockhash", height);
    if (result == null) {
      return null;
    }
    String hash = result.optString(BitcoinRpcClient.RESULT_KEY, null);
    return hash == null || hash.isBlank() ? null : hash;
  }

  /**
   * @param fromBlock a block hash, or {@link JSONObject#NULL} to walk the
   *     wallet's entire history
   */
  private JSONObject listSinceBlock(
    CurrencyEntity currencyEntity,
    Object fromBlock
  ) {
    return rpc.wallet(
      currencyEntity,
      "listsinceblock",
      fromBlock,
      1,
      true,
      true,
      // Change belongs to the hot wallet, never to a user, so leaving it out
      // keeps it from being considered as a deposit at all.
      false
    );
  }

  /**
   * Converts an RPC amount in BTC to satoshis.
   *
   * <p>Routed through the decimal string rather than double arithmetic. org.json
   * may hand back a Double, and scaling one of those by 1e8 directly reintroduces
   * exactly the binary rounding error that makes a satoshi go missing.
   */
  static BigInteger toSatoshis(Object amount) {
    BigDecimal btc = toBigDecimal(amount);
    if (btc == null) {
      return null;
    }
    return btc.movePointRight(8).setScale(0, RoundingMode.HALF_UP).toBigInteger(
    );
  }

  /**
   * Renders satoshis as a plain BTC string. Explicitly not scientific notation:
   * BigDecimal would print a single satoshi as 1E-8, and while this node happens
   * to parse that, a plain string is what the RPC documents and what is legible
   * in a log.
   */
  static String toBitcoin(BigInteger satoshis) {
    return new BigDecimal(satoshis).movePointLeft(8).setScale(8).toPlainString();
  }

  static BigDecimal toBigDecimal(Object value) {
    if (value == null || JSONObject.NULL.equals(value)) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }

}
