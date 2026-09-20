package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.DepositRecordEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.repository.DepositRecordsRepository;
import com.nanobot.nanobotbackend.service.chain.ChainAdapter;
import com.nanobot.nanobotbackend.service.chain.ChainAdapterRegistry;
import com.nanobot.nanobotbackend.service.chain.ChainLedgerService;
import com.nanobot.nanobotbackend.service.chain.NanoAddressIndex;
import com.nanobot.nanobotbackend.service.chain.NanoConfirmationHandler;
import com.nanobot.nanobotbackend.service.chain.NanoWebSocketService;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import uk.oczadly.karl.jnano.model.NanoAccount;

/**
 * Nano-family chain integration: Nano, Banano and any other fork exposing the
 * same node RPC. Also carries the cron entry point that dispatches every
 * currency to its own {@link ChainAdapter}.
 *
 * <p>Deposits are found two ways. The node pushes confirmations over a
 * websocket filtered to the accounts the bot custodies, which is what normally
 * spots a deposit, and a periodic sweep re-reads every account's balance and
 * receivables. The sweep is the authority: websocket delivery is best effort,
 * so anything the socket misses has to be found by asking. The sweep is also
 * the only mechanism for a currency with no websocket configured, which is what
 * makes push adoptable one currency at a time.
 */
@Service
public class NodesServiceImpl
  implements NodesService, ChainAdapter, NanoConfirmationHandler {

  private static final int BATCH_SIZE = 100;

  /**
   * How long between full account sweeps once push notifications are flowing.
   * Deposit latency does not depend on this - the websocket does that - so it
   * only bounds how long a missed notification can go unnoticed.
   */
  private static final long DISCOVERY_INTERVAL_MILLIS = 300_000L;

  /**
   * The node is asked whether it is synced at most this often. It was asked
   * every tick, which for a check that moves at block-count speed was two
   * wasted round trips a second per currency.
   */
  private static final long HEALTH_CHECK_INTERVAL_MILLIS = 30_000L;

  /**
   * Rejected submissions before a withdrawal is refunded rather than retried
   * forever. Matches the Monero and Bitcoin adapters.
   */
  private static final int MAX_SEND_ATTEMPTS = 3;

  /**
   * Attempts between reports once a send is past its limit but cannot be
   * resolved, so a permanently stuck entry stays visible without logging every
   * tick.
   */
  private static final int STUCK_REPORT_INTERVAL = 300;

  private final FileLogger fileLogger;
  private final CoreServices coreServices;
  private final CurrenciesService currenciesService;
  private final MessagesService messagesService;
  private final QueuesService queuesService;
  private final TransactionsService transactionsService;
  private final DepositRecordsRepository depositRecordsRepository;
  private final NanoAddressIndex nanoAddressIndex;
  private final NanoWebSocketService nanoWebSocketService;
  private final HttpClient client;

  /** Ticker to the time its last full sweep finished. */
  private final Map<String, Long> lastDiscoveryMillis = new ConcurrentHashMap<>();

  /**
   * Ticker to when a positive health check stops being trusted. Only successes
   * are cached, so an offline node is noticed on the very next tick instead of
   * up to a health check interval later.
   */
  private final Map<String, Long> healthyUntilMillis = new ConcurrentHashMap<>();

  /** Tickers whose next pass must sweep regardless of the interval. */
  private final Set<String> reconciliationRequests =
    ConcurrentHashMap.newKeySet();

  @Autowired
  private TransferExecutorService transferExecutorService;

  /**
   * Field-injected rather than constructor-injected: the registry collects every
   * ChainAdapter, this class is one of them, and only field injection can
   * resolve that cycle.
   */
  @Autowired
  private ChainAdapterRegistry chainAdapterRegistry;

  /** Field-injected for the same cycle reason as the registry above. */
  @Autowired
  private ChainLedgerService chainLedgerService;

  private final String botUserId = System.getenv("BOT_USER_ID");
  private final String workServerUrl;
  private static final String CONFIRMED_NOTE =
    "has been *confirmed* on the network!";
  private static final String BALANCE_RESPONSE_KEY = "balance";
  private static final String CONFIRMED_RESPONSE_KEY = "confirmed";
  private static final String BLOCK_COUNT_ACTION = "block_count";
  private static final String TELEMETRY_ACTION = "telemetry";
  private static final String BLOCK_INFORMATION_ACTION = "block_info";
  private static final String ACTION_COMMAND = "action";
  /**
   * How far behind the peer median the node may fall before it is treated as
   * out of sync and all deposits and withdrawals are held.
   *
   * <p>This is a broad health signal, not a per-transaction safety check.
   * Deposits are credited from each block's own confirmation status and
   * withdrawals build on the hot wallet's own frontier, which nothing else
   * spends from, so neither depends on the node sitting exactly at the tip.
   *
   * <p>Was 50, which turned out to be inside normal operating noise: a measured
   * episode reached a cemented lag of 169 purely from packet loss on the link to
   * the node, halting both chains while the node was in fact perfectly usable. A
   * node that is genuinely desynced - bootstrapping, stalled, forked - sits
   * thousands to millions of blocks behind, so this still catches that while
   * ignoring a slow minute. It is also self-scaling in the right direction:
   * during a burst these are seconds of drift, during a quiet spell minutes.
   */
  private static final int MAX_TELEMETRY_LAG = 500;

  private static final int MIN_TELEMETRY_PEERS = 3;

  @Autowired
  public NodesServiceImpl(
    CurrenciesService currenciesService,
    MessagesService messagesService,
    QueuesService queuesService,
    CoreServices coreServices,
    TransactionsService transactionsService,
    DepositRecordsRepository depositRecordsRepository,
    NanoAddressIndex nanoAddressIndex,
    NanoWebSocketService nanoWebSocketService
  ) {
    this(
      currenciesService,
      messagesService,
      queuesService,
      coreServices,
      transactionsService,
      depositRecordsRepository,
      nanoAddressIndex,
      nanoWebSocketService,
      HttpClient.newHttpClient(),
      System.getenv("WORK_SERVER_URL")
    );
  }

  /**
   * Constructor for testing: allows injecting HttpClient and work server URL
   * so that MockWebServer or other test doubles can be used.
   */
  NodesServiceImpl(
    CurrenciesService currenciesService,
    MessagesService messagesService,
    QueuesService queuesService,
    CoreServices coreServices,
    TransactionsService transactionsService,
    DepositRecordsRepository depositRecordsRepository,
    NanoAddressIndex nanoAddressIndex,
    NanoWebSocketService nanoWebSocketService,
    HttpClient client,
    String workServerUrl
  ) {
    this.fileLogger = new FileLogger("NodesService");
    this.currenciesService = currenciesService;
    this.messagesService = messagesService;
    this.coreServices = coreServices;
    this.transactionsService = transactionsService;
    this.queuesService = queuesService;
    this.depositRecordsRepository = depositRecordsRepository;
    this.nanoAddressIndex = nanoAddressIndex;
    this.nanoWebSocketService = nanoWebSocketService;
    this.client = client;
    this.workServerUrl = workServerUrl;
  }

  public static int getCharsBeforeUnderscore(String input) {
    // Find position of underscore
    int underscoreIndex = input.indexOf('_');
    return (underscoreIndex >= 0) ? underscoreIndex : 0;
  }

  public static byte[] addressToPublicKey(
    CurrencyEntity currencyEntity,
    String address
  ) {
    String raw = address.substring(getCharsBeforeUnderscore(address) + 1);

    // Remove checksum (last 8 chars)
    String withoutChecksum = raw.substring(0, raw.length() - 8);

    // Base32 decode (Banano uses a custom Base32 alphabet)
    String alphabet = "13456789abcdefghijkmnopqrstuwxyz";
    BigInteger result = BigInteger.ZERO;
    for (char c : withoutChecksum.toCharArray()) {
      int val = alphabet.indexOf(c);
      if (val == -1) throw new IllegalArgumentException(
        "Invalid char in address"
      );
      result = result.shiftLeft(5).add(BigInteger.valueOf(val));
    }

    // Convert to 32-byte array
    byte[] bytes = result.toByteArray();

    // BigInteger may add a leading 0 for sign, ensure exactly 32 bytes
    byte[] publicKey = new byte[32];
    int copyStart = Math.max(0, bytes.length - 32);
    int copyLength = Math.min(32, bytes.length);
    System.arraycopy(bytes, copyStart, publicKey, 32 - copyLength, copyLength);

    return publicKey;
  }

  public JSONObject getWorkBody(String hash, String difficulty) {
    JSONObject requestWorkBody = new JSONObject()
      .put(ACTION_COMMAND, "work_generate")
      .put("hash", hash)
      .put("difficulty", difficulty);

    return requestWorkBody;
  }

  /**
   * A node response rendered for a log line, tolerating null.
   *
   * <p>These are read on the failure branch, where the response being null is
   * one of the things being reported. Calling toString or getString on it there
   * threw a NullPointerException from inside the error handler, losing the
   * original failure and taking the rest of the pass with it.
   */
  private static String describeResponse(JSONObject response) {
    return response == null ? "no response" : response.toString();
  }

  /**
   * Proof of work for a block, or null when the work server did not produce
   * any.
   *
   * <p>Callers previously read the value straight out of the response, so an
   * unreachable or slow work server threw a NullPointerException. That escaped
   * the cron loop and abandoned the rest of the pass, meaning a Nano work
   * outage also stopped Monero and Bitcoin from being processed. Returning null
   * leaves the entry queued for the next tick instead.
   */
  private String resolveWork(
    String hash,
    String difficulty,
    QueueEntity queueEntity
  ) {
    JSONObject workResponse = sendRequestToWorkServer(
      workServerUrl,
      getWorkBody(hash, difficulty)
    );
    if (workResponse == null || !workResponse.has("work")) {
      fileLogger.error(
        "No work returned for " +
        queueEntity.getTicker() +
        " queue #" +
        queueEntity.getId() +
        "; leaving it queued to retry."
      );
      return null;
    }
    return workResponse.getString("work");
  }

  @Override
  public void processActivity(
    UserDetailsEntity botUserDetails,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    String botUserSeed = botUserDetails.getSeed();
    String currencyTicker = currencyEntity.getTicker();

    if (!isNodeHealthy(currencyEntity)) {
      return;
    }

    //this is the consolidation account, the hot wallet
    String botUserAddress = CryptoUtil.deriveAddress(
      botUserDetails,
      currencyTicker
    );

    if (currencyEntity.getProcessWithdrawals()) {
      processWithdrawals(currencyEntity, commandMap);
    } else {
      fileLogger.warn(
        "Not currently processing withdrawals for " + currencyTicker + "!"
      );
    }
    if (currencyEntity.getProcessDeposits()) {
      processDeposits(currencyEntity, botUserAddress, botUserSeed, commandMap);
    } else {
      fileLogger.warn(
        "Not currently processing deposits for " + currencyTicker + "!"
      );
    }

    // Last, because the subscription filter is built from the address index and
    // the sweep above is what fills it.
    nanoWebSocketService.ensureSubscribed(currencyEntity);
  }

  /**
   * Whether the node is online and keeping up with its peers, re-asking at most
   * every {@link #HEALTH_CHECK_INTERVAL_MILLIS}.
   */
  private boolean isNodeHealthy(CurrencyEntity currencyEntity) {
    String ticker = currencyEntity.getTicker();
    long now = System.currentTimeMillis();
    Long trustedUntil = healthyUntilMillis.get(ticker);
    if (trustedUntil != null && now < trustedUntil) {
      return true;
    }

    boolean healthy = sendRequestToNode(
      null,
      null,
      null,
      currencyEntity,
      BLOCK_COUNT_ACTION,
      new JSONObject().put(ACTION_COMMAND, BLOCK_COUNT_ACTION)
    );

    if (healthy) {
      healthyUntilMillis.put(ticker, now + HEALTH_CHECK_INTERVAL_MILLIS);
    } else {
      healthyUntilMillis.remove(ticker);
      fileLogger.warn(ticker + " node is OFFLINE!");
    }
    return healthy;
  }

  /**
   * Whether this pass should re-read every account from the node.
   *
   * <p>Without a live subscription this is always true, which is exactly the
   * poll-every-tick behaviour a currency had before websockets existed.
   */
  private boolean isDiscoveryDue(CurrencyEntity currencyEntity) {
    String ticker = currencyEntity.getTicker();
    if (reconciliationRequests.remove(ticker)) {
      return true;
    }
    if (!nanoWebSocketService.isSubscribed(ticker)) {
      return true;
    }
    Long last = lastDiscoveryMillis.get(ticker);
    return (
      last == null || System.currentTimeMillis() - last >= DISCOVERY_INTERVAL_MILLIS
    );
  }

  @Override
  public void requestReconciliation(String ticker) {
    if (ticker != null) {
      reconciliationRequests.add(ticker);
    }
  }

  public void processSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity
  ) {
    processSend(queueEntity, currencyEntity, null);
  }

  /**
   * @param commandMap slash command ids for the notification sent if the
   *     withdrawal is ultimately refunded; null skips the refund, which is what
   *     callers with no user context want
   */
  public void processSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    // A hash recorded on an unprocessed entry means an earlier pass built this
    // block and may have published it before it could record having done so.
    if (queueEntity.getBlockHash() != null) {
      Publication published = findPublishedBlock(
        currencyEntity,
        queueEntity.getBlockHash()
      );
      if (published == Publication.UNKNOWN) {
        fileLogger.warn(
          "Cannot tell whether block " +
          queueEntity.getBlockHash() +
          " for queue #" +
          queueEntity.getId() +
          " was published; deferring rather than risking a double send."
        );
        return;
      }
      if (published == Publication.PUBLISHED) {
        fileLogger.warn(
          "Recovered an already published " +
          queueEntity.getTicker() +
          " send instead of rebuilding it: " +
          queueEntity.getBlockHash()
        );
        queueEntity.setProcessed(true);
        queuesService.updateQueueProgress(
          queueEntity.getId(),
          null,
          null,
          null,
          true
        );
        return;
      }
      // Not published. Rebuilding is safe, and because nothing landed the
      // frontier is unchanged, so the block built below is byte for byte the
      // one already recorded.
    }

    // get the account info
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "account_info")
        .put("representative", true)
        .put("account", queueEntity.getSourceAddress())
    );

    if (
      response == null ||
      (response.has("error") &&
        "Account not found".equals(response.getString("error")))
    ) {
      fileLogger.error(
        "Failed to get account info for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node."
      );
      // An unreachable node says nothing about whether this send can ever
      // succeed, so it must not burn an attempt. A node that answered and said
      // the account does not exist is a permanent condition.
      if (response != null) {
        handleSendFailure(queueEntity, currencyEntity, commandMap);
      }
      return;
    }

    // generate the work
    String work = resolveWork(
      response.getString("frontier"),
      currencyEntity.getSendDifficulty(),
      queueEntity
    );
    if (work == null) {
      return;
    }

    byte[] privateKey = resolveSigningKey(queueEntity);

    // send create block request
    JSONObject createBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "block_create")
        .put("json_block", "true")
        .put("type", "state")
        .put(
          BALANCE_RESPONSE_KEY,
          new BigInteger(response.getString(BALANCE_RESPONSE_KEY))
            .subtract(new BigInteger(queueEntity.getRaw()))
            .toString()
        )
        .put("key", bytesToHex(privateKey))
        .put("representative", response.getString("representative"))
        .put(
          "link",
          bytesToHex(
            addressToPublicKey(currencyEntity, queueEntity.getTargetAddress())
          )
        )
        .put("previous", response.getString("frontier"))
        .put("work", work)
    );

    if (createBlockResponse == null || (createBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to create block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        describeResponse(createBlockResponse)
      );
      if (createBlockResponse != null) {
        handleSendFailure(queueEntity, currencyEntity, commandMap);
      }
      return;
    }

    JSONObject blockObject = createBlockResponse.getJSONObject("block");

    // Record the hash before publishing, never after. A state block's hash is
    // fully determined by its contents, so block_create already knows what
    // process is about to return. Writing it first is what makes the recovery
    // above possible: without it, a crash between publishing and saving leaves
    // no trace of the block, and the next pass rebuilds from the new frontier
    // and pays the user a second time.
    String intendedHash = createBlockResponse.optString("hash", null);
    if (intendedHash == null || intendedHash.isBlank()) {
      fileLogger.error(
        "block_create returned no hash for queue #" +
        queueEntity.getId() +
        "; refusing to publish a block that could not be recovered."
      );
      return;
    }
    if (
      !queuesService.updateQueueProgress(
        queueEntity.getId(),
        null,
        null,
        intendedHash,
        null
      )
    ) {
      fileLogger.error(
        "Could not record the pending block hash for queue #" +
        queueEntity.getId() +
        "; refusing to publish, since a crash afterwards would be invisible."
      );
      return;
    }
    queueEntity.setBlockHash(intendedHash);

    // send process block request
    JSONObject processBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "process")
        .put("json_block", "true")
        .put("block", blockObject)
    );

    if (processBlockResponse == null || (processBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to process send block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        describeResponse(processBlockResponse)
      );
      // A rejected block is a permanent condition. A timeout is not, and it is
      // also the case where the block may still be propagating, which is why the
      // hash was recorded before this call and is checked on the way back in.
      if (processBlockResponse != null) {
        handleSendFailure(queueEntity, currencyEntity, commandMap);
      }
      return;
    }

    String publishedHash = processBlockResponse.optString("hash", intendedHash);
    if (!intendedHash.equals(publishedHash)) {
      // Should be impossible, and worth knowing about if it ever happens: the
      // recorded hash is what recovery looks for, so a mismatch would make a
      // published block unrecoverable.
      fileLogger.error(
        "Published block hash " +
        publishedHash +
        " does not match the recorded " +
        intendedHash +
        " for queue #" +
        queueEntity.getId()
      );
    }

    // if successful update to processed and add xid
    queueEntity.setBlockHash(publishedHash);
    queueEntity.setProcessed(true);
    queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));

    fileLogger.info(
      "Successfully processed send transaction: " + queueEntity.getBlockHash()
    );

    // Announced even though confirmation usually follows within a second or
    // two: the pair of messages shows how fast the network is, and when the
    // node is slow to see the confirmation the user still has proof that their
    // /send went out. Only on a fresh publish; the recovery path above cannot
    // tell whether this was already announced before the crash. Bot-owned
    // sends have nobody to tell, matching confirmSend.
    if (
      queueEntity.getUserId() != null &&
      !queueEntity.getUserId().equals(botUserId)
    ) {
      // Nano is quorum confirmed, so a single confirmation is final.
      chainLedgerService.notifyWithdrawalSent(
        currencyEntity,
        queueEntity,
        1,
        commandMap
      );
    }
  }

  /**
   * Counts a rejected send and, once attempts are exhausted, refunds the user
   * rather than leaving the balance debited forever.
   *
   * <p>Only called for failures the node actually answered with. A timeout or
   * an unreachable node is not evidence that the send can never work, and
   * counting those would let a brief outage cancel a perfectly good withdrawal
   * within three seconds.
   *
   * <p>Nano is feeless, so unlike Monero and Bitcoin the usual cause is not an
   * amount too small to cover a fee. It is a block the node will not accept -
   * a fork, a stale frontier, a missing account - all of which fail the same
   * way on every retry.
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

    if (attempts <= MAX_SEND_ATTEMPTS) {
      fileLogger.error(
        "Failed to publish " +
        queueEntity.getTicker() +
        " send for queue #" +
        queueEntity.getId() +
        " (attempt " +
        attempts +
        " of " +
        MAX_SEND_ATTEMPTS +
        ")"
      );
    }

    if (attempts < MAX_SEND_ATTEMPTS) {
      return;
    }

    // Past the limit this runs every tick, so the terminal states report on the
    // threshold and then sparingly, rather than once a second forever.
    boolean report =
      attempts == MAX_SEND_ATTEMPTS || attempts % STUCK_REPORT_INTERVAL == 0;

    // A sweep moves the bot's own funds out of a user's deposit address. There
    // is no debited balance behind it and so nothing to refund; the money is
    // still on chain and the next sweep will find it.
    if (
      queueEntity.getUserId() == null ||
      queueEntity.getUserId().equals(botUserId)
    ) {
      if (report) {
        fileLogger.error(
          "Sweep from " +
          queueEntity.getSourceAddress() +
          " has failed " +
          attempts +
          " times. No user balance is affected, but the funds are not " +
          "consolidated."
        );
      }
      return;
    }

    // Never refund without proving nothing reached the network. The recorded
    // hash makes that exact here: a state block either exists under that hash
    // or it does not.
    if (queueEntity.getBlockHash() != null) {
      Publication published = findPublishedBlock(
        currencyEntity,
        queueEntity.getBlockHash()
      );
      if (published == Publication.UNKNOWN) {
        if (report) {
          fileLogger.error(
            "Withdrawal for queue #" +
            queueEntity.getId() +
            " has exhausted its attempts, but the node cannot confirm whether " +
            "block " +
            queueEntity.getBlockHash() +
            " was published. Holding rather than refunding blind."
          );
        }
        return;
      }
      if (published == Publication.PUBLISHED) {
        fileLogger.warn(
          "Withdrawal for queue #" +
          queueEntity.getId() +
          " did reach the network as " +
          queueEntity.getBlockHash() +
          "; recording it instead of refunding."
        );
        queueEntity.setProcessed(true);
        queuesService.updateQueueProgress(
          queueEntity.getId(),
          null,
          null,
          null,
          true
        );
        return;
      }
    }

    // Either no block was ever built, or one was built and proven absent from
    // the network. Nothing was sent, so the debit can be reversed.
    if (commandMap == null) {
      if (report) {
        fileLogger.error(
          "Withdrawal for queue #" +
          queueEntity.getId() +
          " is refundable but was reached without command context; holding."
        );
      }
      return;
    }

    String reason =
      "## The network rejected this transaction, so it could not be sent.\n" +
      "-# Your funds were **not** sent and have been returned to your balance. " +
      "Please try again, and let us know if it keeps happening.";

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

  /** Whether a block is known to the network, used to tell a published send from one that never went out. */
  private enum Publication {
    PUBLISHED,
    NOT_PUBLISHED,
    /**
     * The node could not be asked. Distinct from NOT_PUBLISHED because
     * rebuilding is only safe in the second case; treating an outage as "never
     * sent" is exactly how a withdrawal gets paid twice.
     */
    UNKNOWN,
  }

  private Publication findPublishedBlock(
    CurrencyEntity currencyEntity,
    String hash
  ) {
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
        .put("hash", hash)
    );
    if (response == null) {
      return Publication.UNKNOWN;
    }
    if (response.has("error")) {
      // Only this one error means the block genuinely is not there. Anything
      // else is the node declining to answer.
      return "Block not found".equals(response.optString("error"))
        ? Publication.NOT_PUBLISHED
        : Publication.UNKNOWN;
    }
    return response.has("contents")
      ? Publication.PUBLISHED
      : Publication.UNKNOWN;
  }

  public void confirmSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    confirmSend(queueEntity, currencyEntity, commandMap, false);
  }

  /**
   * @param alreadyConfirmed set when the caller has proof the block is
   *     confirmed, which a websocket notification is, so the node is not asked
   *     to repeat what it just said.
   */
  public void confirmSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap,
    boolean alreadyConfirmed
  ) {
    if (
      !alreadyConfirmed &&
      !isBlockConfirmed(currencyEntity, queueEntity.getBlockHash())
    ) {
      return;
    }
    Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deletedQueue.isPresent()) {
      fileLogger.info(
        "Successfully removed confirmed send - queue #" + queueEntity.getId()
      );

      if (
        queueEntity.getUserId() == null ||
        queueEntity.getUserId().equals(botUserId)
      ) {
        return;
      }

      transactionsService.updateTransactionBlockHash(
        queueEntity.getTransactionId(),
        queueEntity.getBlockHash()
      );

      String decimalValue = currenciesService.getCurrencyDecimalValue(
        queueEntity.getRaw(),
        Integer.valueOf(currencyEntity.getPrecision())
      );

      String messageDescriptionTitle =
        "<@" +
        queueEntity.getUserId() +
        ">'s withdrawal " +
        CONFIRMED_NOTE +
        "\n";

      String commandNote =
        "\n-# Use </transactions:" +
        commandMap.get("transactions") +
        "> to view your transaction history." +
        "\n";

      String messageDescriptionContent =
        "### 💸 __Currency Transfers__\n> **" +
        decimalValue +
        " " +
        currencyEntity.getTicker() +
        "** (≈$" +
        currenciesService.getCurrencyDollarValue(
          decimalValue,
          currencyEntity.getValue()
        ) +
        ") " +
        currencyEntity.getEmoji() +
        "\n" +
        "### 🔗 __Block Hash__\n> `" +
        queueEntity.getBlockHash() +
        "`" +
        "\n" +
        "### 📌 __Withdrawal Address__\n> `" +
        queueEntity.getTargetAddress() +
        "`";

      String userMessageDescription =
        messageDescriptionTitle + commandNote + messageDescriptionContent;

      String systemMessageDescription =
        messageDescriptionTitle + messageDescriptionContent;

      messagesService.createMessage(
        new MessageDto(
          null,
          System.getenv("HOME_SERVER_ID"),
          System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
          null,
          "🧾 Withdrawal Confirmed",
          currencyEntity.getColor(),
          systemMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          null
        )
      );

      messagesService.createMessage(
        new MessageDto(
          queueEntity.getUserId(),
          null,
          null,
          null,
          "🧾 Withdrawal Confirmed",
          currencyEntity.getColor(),
          userMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          null
        )
      );
    }
  }

  public void processUpdate(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    Map<String, String> commandMap
  ) {
    // get the account info
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "account_info")
        .put("representative", true)
        .put("account", queueEntity.getSourceAddress())
    );

    if (
      response == null ||
      (response.has("error") &&
        "Account not found".equals(response.getString("error")))
    ) {
      Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
        queueEntity.getId()
      );
      if (deletedQueue.isPresent()) {
        fileLogger.info(
          "Successfully removed confirmed update - queue #" +
          queueEntity.getId()
        );

        String depositAddressDescription =
          "### 🏠︎ __Deposit Address__\n> `" +
          queueEntity.getSourceAddress() +
          "`";

        String userMessageDescription =
          "<@" +
          queueEntity.getUserId() +
          ">'s update request *unsuccessfully* completed.\n" +
          "## Your representative update to `" +
          queueEntity.getTargetAddress() +
          "` failed to process because you have not sent " +
          currencyEntity.getName() +
          " to your **" +
          queueEntity.getTicker() +
          "** " +
          currencyEntity.getEmoji() +
          " deposit address.\n" +
          "### This is a one time process, and can be achieved by using </" +
          Constants.COMMAND_NAME_SEND +
          ":" +
          commandMap.get("send") +
          "> to withdraw as little as " +
          currenciesService.getCurrencyDecimalValue(
            currencyEntity.getMinimumWithdraw(),
            Integer.valueOf(currencyEntity.getPrecision())
          ) +
          " " +
          currencyEntity.getTicker() +
          " (" +
          currencyEntity.getPrecision() +
          " decimal places) to your deposit address." +
          "\n" +
          depositAddressDescription;

        String systemMessageDescription =
          "<@" +
          queueEntity.getUserId() +
          ">'s update request *unsuccessfully* completed due to their deposit account being unopened." +
          "\n" +
          depositAddressDescription;

        messagesService.createMessage(
          new MessageDto(
            null,
            System.getenv("HOME_SERVER_ID"),
            System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
            null,
            "💀 Update Failed",
            currencyEntity.getColor(),
            systemMessageDescription,
            new java.util.Date(),
            "https://nanexplorer.com/" +
            currencyEntity.getName().toLowerCase() +
            "/accounts/" +
            queueEntity.getSourceAddress(),
            null,
            null
          )
        );

        messagesService.createMessage(
          new MessageDto(
            queueEntity.getUserId(),
            null,
            null,
            null,
            "💀 Update Failed",
            currencyEntity.getColor(),
            userMessageDescription,
            new java.util.Date(),
            "https://nanexplorer.com/" +
            currencyEntity.getName().toLowerCase() +
            "/accounts/" +
            queueEntity.getSourceAddress(),
            null,
            null
          )
        );

        return;
      } else {
        return;
      }
    }

    // generate the work
    String work = resolveWork(
      response.getString("frontier"),
      currencyEntity.getSendDifficulty(),
      queueEntity
    );
    if (work == null) {
      return;
    }

    byte[] privateKey = resolveSigningKey(queueEntity);

    // send create block request
    JSONObject createBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "block_create")
        .put("json_block", "true")
        .put("type", "state")
        .put(BALANCE_RESPONSE_KEY, response.getString(BALANCE_RESPONSE_KEY))
        .put("key", bytesToHex(privateKey))
        .put("representative", queueEntity.getTargetAddress())
        .put(
          "link",
          "0000000000000000000000000000000000000000000000000000000000000000"
        )
        .put("previous", response.getString("frontier"))
        .put("work", work)
    );

    if (createBlockResponse == null || (createBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to create block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node."
      );
      return;
    }

    JSONObject blockObject = createBlockResponse.getJSONObject("block");

    // send process block request
    JSONObject processBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "process")
        .put("json_block", "true")
        .put("block", blockObject)
    );

    if (processBlockResponse == null || (processBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to process update block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        describeResponse(processBlockResponse)
      );
      return;
    }

    // if successful update to processed and add xid
    queueEntity.setBlockHash(processBlockResponse.getString("hash"));
    queueEntity.setProcessed(true);
    queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));

    fileLogger.info(
      "Successfully processed update block: " + queueEntity.getBlockHash()
    );
  }

  public void confirmUpdate(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    confirmUpdate(queueEntity, currencyEntity, commandMap, false);
  }

  public void confirmUpdate(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap,
    boolean alreadyConfirmed
  ) {
    if (
      !alreadyConfirmed &&
      !isBlockConfirmed(currencyEntity, queueEntity.getBlockHash())
    ) {
      return;
    }
    Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deletedQueue.isPresent()) {
      fileLogger.info(
        "Successfully removed confirmed update - queue #" + queueEntity.getId()
      );

      String messageDescription =
        "<@" +
        queueEntity.getUserId() +
        ">'s " +
        queueEntity.getTicker() +
        " " +
        currencyEntity.getEmoji() +
        " update " +
        CONFIRMED_NOTE +
        "\n" +
        "### 📌 __Representative Address__\n> `" +
        queueEntity.getTargetAddress() +
        "`" +
        "\n" +
        "### 🔗 __Block Hash__\n> `" +
        queueEntity.getBlockHash() +
        "`";

      messagesService.createMessage(
        new MessageDto(
          null,
          System.getenv("HOME_SERVER_ID"),
          System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
          null,
          "🧾 Update Confirmed",
          currencyEntity.getColor(),
          messageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          null
        )
      );

      messagesService.createMessage(
        new MessageDto(
          queueEntity.getUserId(),
          null,
          null,
          null,
          "🧾 Update Confirmed",
          currencyEntity.getColor(),
          messageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          null
        )
      );
    }
  }

  public void processReceive(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity
  ) {
    // 1. Get account info
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "account_info")
        .put("representative", true)
        .put("account", queueEntity.getTargetAddress())
    );

    fileLogger.info(
      "Entity ID: " +
      queueEntity.getId() +
      " Process Block Response: " +
      describeResponse(response)
    );

    BigInteger currentBalance = BigInteger.ZERO;
    String previous;
    String workHash;
    String difficulty;

    // 2. Derive private key
    byte[] privateKey = resolveSigningKey(queueEntity);

    // 3. Determine if account exists
    if (
      response == null ||
      (response.has("error") &&
        "Account not found".equals(response.getString("error")))
    ) {
      fileLogger.info(
        "Account " +
        queueEntity.getTargetAddress() +
        " does not exist yet. This will be the first receive block."
      );
      previous =
        "0000000000000000000000000000000000000000000000000000000000000000";
      workHash = bytesToHex(
        addressToPublicKey(currencyEntity, queueEntity.getTargetAddress())
      );
      difficulty = currencyEntity.getOpenDifficulty();
    } else {
      currentBalance = new BigInteger(response.getString(BALANCE_RESPONSE_KEY));
      previous = response.getString("frontier");
      workHash = previous; // generate work on frontier
      difficulty = currencyEntity.getReceiveDifficulty();
    }

    String work = resolveWork(workHash, difficulty, queueEntity);
    if (work == null) {
      return;
    }

    // 5. Determine new balance
    BigInteger newBalance = currentBalance.add(
      new BigInteger(queueEntity.getRaw())
    );

    // 6. Create the receive block
    JSONObject createBlockRequest = new JSONObject()
      .put(ACTION_COMMAND, "block_create")
      .put("json_block", "true")
      .put("type", "state")
      .put(BALANCE_RESPONSE_KEY, newBalance.toString())
      .put("key", bytesToHex(privateKey))
      .put(
        "representative",
        response.has("representative")
          ? response.getString("representative")
          : queueEntity.getTargetAddress()
      )
      .put("link", queueEntity.getBlockHash()) // pending send block
      .put("previous", previous) // always include previous
      .put("work", work);

    JSONObject createBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      createBlockRequest
    );

    if (createBlockResponse == null || createBlockResponse.has("error")) {
      fileLogger.error(
        "Failed to create receive block for " +
        queueEntity.getTargetAddress() +
        " on " +
        queueEntity.getTicker() +
        " node."
      );
      return;
    }

    JSONObject blockObject = createBlockResponse.getJSONObject("block");

    JSONObject processBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "process")
        .put("json_block", "true")
        .put("block", blockObject)
    );

    if (processBlockResponse == null || processBlockResponse.has("error")) {
      fileLogger.error(
        "Failed to process receive block for " +
        queueEntity.getTargetAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        describeResponse(processBlockResponse)
      );
      return;
    }

    queueEntity.setProcessed(true);
    queueEntity.setBlockHash(processBlockResponse.getString("hash"));
    queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));

    fileLogger.info(
      "Successfully processed receive block: " + queueEntity.getBlockHash()
    );
  }

  public void confirmReceive(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    confirmReceive(queueEntity, currencyEntity, commandMap, false);
  }

  public void confirmReceive(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap,
    boolean alreadyConfirmed
  ) {
    if (
      !alreadyConfirmed &&
      !isBlockConfirmed(currencyEntity, queueEntity.getBlockHash())
    ) {
      return;
    }
    Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deletedQueue.isPresent()) {
      fileLogger.info(
        "Successfully removed confirmed receive - queue #" + queueEntity.getId()
      );

      if (
        queueEntity.getUserId() == null ||
        queueEntity.getUserId().equals(botUserId)
      ) {
        return;
      }

      Optional<DepositRecordEntity> depositRecord = claimDeposit(
        queueEntity,
        currencyEntity
      );
      if (depositRecord.isEmpty()) {
        return;
      }

      // credit the user
      TransferResponseDto transferResponseDto = new TransferResponseDto();
      coreServices.commandsService.setCommands(
        Constants.COMMAND_NAME_RECEIVE,
        queueEntity.getUserId(),
        transferResponseDto::setCommands
      );

      WalletDto walletDto = new WalletDto(
        queueEntity.getTicker(),
        queueEntity.getRaw(),
        true
      );
      List<WalletDto> wallets = new ArrayList<>();
      wallets.add(walletDto);
      TransferDto transferDto = new TransferDto(wallets, new ArrayList<>());
      transferResponseDto.setPrimaryTransfer(transferDto);

      transferResponseDto.setBlockHash(queueEntity.getBlockHash());

      TransferResponseDto transfer = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_RECEIVE,
        null,
        null,
        "0",
        null,
        Collections.singletonList(queueEntity.getUserId()),
        null,
        transferResponseDto
      );

      // Ties the guard record to the ledger entry it produced, so a record left
      // without one is visibly a credit that did not finish.
      DepositRecordEntity record = depositRecord.get();
      record.setTransactionId(transfer.getTransactionId());
      depositRecordsRepository.save(record);

      String footerContent =
        "Nanobot Transaction ID " + transfer.getTransactionId();

      String depositConfirmedTitle = "🧾 Deposit Confirmed";

      String decimalValue = currenciesService.getCurrencyDecimalValue(
        queueEntity.getRaw(),
        Integer.valueOf(currencyEntity.getPrecision())
      );

      String messageDescriptionTitle =
        "<@" + queueEntity.getUserId() + ">'s deposit " + CONFIRMED_NOTE + "\n";

      String commandNote =
        "\n-# Use </transactions:" +
        commandMap.get("transactions") +
        "> to view your transaction history." +
        "\n" +
        "-# Use </wallet:" +
        commandMap.get("wallet") +
        "> to view your *updated* currency balances." +
        "\n";

      String messageDescriptionContent =
        "### 💼 __Wallet Credits__\n> **" +
        decimalValue +
        " " +
        currencyEntity.getTicker() +
        "** (≈$" +
        currenciesService.getCurrencyDollarValue(
          decimalValue,
          currencyEntity.getValue()
        ) +
        ") " +
        currencyEntity.getEmoji() +
        "\n" +
        "### 🏠︎ __Deposit Address__\n> `" +
        queueEntity.getTargetAddress() +
        "`" +
        "\n" +
        "### 🔗 __Block Hash__\n> `" +
        queueEntity.getBlockHash() +
        "`";

      String userMessageDescription =
        messageDescriptionTitle + commandNote + messageDescriptionContent;

      String systemMessageDescription =
        messageDescriptionTitle + messageDescriptionContent;

      messagesService.createMessage(
        new MessageDto(
          null,
          System.getenv("HOME_SERVER_ID"),
          System.getenv("DEPOSIT_LOGGING_CHANNEL_ID"),
          null,
          depositConfirmedTitle,
          currencyEntity.getColor(),
          systemMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          footerContent
        )
      );

      messagesService.createMessage(
        new MessageDto(
          queueEntity.getUserId(),
          null,
          null,
          null,
          depositConfirmedTitle,
          currencyEntity.getColor(),
          userMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          footerContent
        )
      );
    }
  }

  public void processWithdrawals(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    List<QueueEntity> queues = queuesService.getQueuesByTicker(
      currencyEntity.getTicker()
    );
    for (QueueEntity queueEntity : queues) {
      if (
        queueEntity.getLevel() == LevelDto.SEND && !queueEntity.getProcessed()
      ) {
        processSend(queueEntity, currencyEntity, commandMap);
      } else if (
        queueEntity.getLevel() == LevelDto.SEND && queueEntity.getProcessed()
      ) {
        confirmSend(queueEntity, currencyEntity, commandMap);
      } else if (
        queueEntity.getLevel() == LevelDto.UPDATE &&
        !queueEntity.getProcessed()
      ) {
        processUpdate(currencyEntity, queueEntity, commandMap);
      } else if (
        queueEntity.getLevel() == LevelDto.UPDATE && queueEntity.getProcessed()
      ) {
        confirmUpdate(queueEntity, currencyEntity, commandMap);
      }
    }
  }

  public void processDeposits(
    CurrencyEntity currencyEntity,
    String botUserAddress,
    String botUserSeed,
    Map<String, String> commandMap
  ) {
    if (isDiscoveryDue(currencyEntity)) {
      discoverDeposits(currencyEntity, botUserAddress, botUserSeed);
    }
    drainDepositQueue(currencyEntity, commandMap);
  }

  /**
   * Asks the node about every custodied account and queues whatever it finds.
   *
   * <p>This is the expensive half: one pass loads every user, derives their
   * address and issues two RPC calls per hundred accounts. It used to run every
   * second, which made node load a function of user count. Websocket
   * notifications now cover the normal case, leaving this as the reconciliation
   * pass that catches whatever push missed.
   */
  void discoverDeposits(
    CurrencyEntity currencyEntity,
    String botUserAddress,
    String botUserSeed
  ) {
    String ticker = currencyEntity.getTicker();

    // Rebuilding the index and building the scan batches share this one
    // derivation pass, which is the only part that costs real CPU.
    Map<String, UserDetailsEntity> allAddresses = nanoAddressIndex.rebuild(
      ticker,
      coreServices.userDetailsService.getUsersDetails(null)
    );

    Map<String, UserDetailsEntity> batch = new LinkedHashMap<>();
    for (Map.Entry<String, UserDetailsEntity> entry : allAddresses.entrySet()) {
      batch.put(entry.getKey(), entry.getValue());
      if (batch.size() >= BATCH_SIZE) {
        processAddressBatch(batch, currencyEntity, botUserAddress, botUserSeed);
        batch.clear();
      }
    }

    if (!batch.isEmpty()) {
      processAddressBatch(batch, currencyEntity, botUserAddress, botUserSeed);
    }

    lastDiscoveryMillis.put(ticker, System.currentTimeMillis());
  }

  /**
   * Advances every queued deposit one step. Bounded by the number of deposits
   * in flight rather than by user count, so this stays on the fast tick.
   */
  void drainDepositQueue(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    for (QueueEntity queueEntity : queuesService.getQueuesByTicker(
      currencyEntity.getTicker()
    )) {
      if (queueEntity.getLevel() != LevelDto.RECEIVE) {
        continue;
      }
      if (queueEntity.getProcessed()) {
        confirmReceive(queueEntity, currencyEntity, commandMap);
      } else {
        processReceive(queueEntity, currencyEntity);
      }
    }
  }

  @Override
  public void checkActivity() {
    // get the bot user details
    Optional<UserDetailsEntity> userDetailsOptional =
      coreServices.userDetailsService.getUserDetailsByUserId(botUserId);
    if (!userDetailsOptional.isPresent()) {
      fileLogger.error(
        "Bot user details not found, cannot proceed with node checks."
      );
      return;
    }
    UserDetailsEntity botUserDetails = userDetailsOptional.get();

    Map<String, String> commandMap = buildCommandMap();

    List<CurrencyEntity> currencies = currenciesService.getCurrencies(null);
    for (CurrencyEntity currencyEntity : currencies) {
      Optional<ChainAdapter> adapter = chainAdapterRegistry.get(currencyEntity);
      if (adapter.isEmpty()) {
        fileLogger.error(
          "No chain adapter registered for protocol " +
          currencyEntity.getProtocol() +
          " (" +
          currencyEntity.getTicker() +
          "); skipping."
        );
        continue;
      }
      // One currency must not be able to starve the others. Every adapter runs
      // from this single loop, so an exception escaping here - an unreachable
      // work server, a malformed node response - would abandon the rest of the
      // pass, and with them any deposit or withdrawal on an unrelated chain.
      try {
        adapter
          .get()
          .processActivity(botUserDetails, currencyEntity, commandMap);
      } catch (Exception e) {
        fileLogger.error(
          "Unhandled error processing " +
          currencyEntity.getTicker() +
          "; continuing with the remaining currencies: " +
          e
        );
      }
    }
  }

  /** Slash command ids, used to build the links in Discord notifications. */
  private Map<String, String> buildCommandMap() {
    List<CommandEntity> entityCommands =
      coreServices.commandsService.getCommands(null);

    List<CommandDto> commands = new ArrayList<>();
    for (CommandEntity commandEntity : entityCommands) {
      commands.add(new CommandDto(commandEntity));
    }

    return commands
      .stream()
      .collect(Collectors.toMap(CommandDto::getName, CommandDto::getCommandId));
  }

  @Override
  public String protocol() {
    return Constants.PROTOCOL_NANO;
  }

  @Override
  public boolean supportsRepresentative() {
    return true;
  }

  /** Nano and its forks are feeless, so there is no estimate to wait for. */
  @Override
  public boolean hasNetworkFee() {
    return false;
  }

  @Override
  public String resolveDepositAddress(
    UserDetailsEntity userDetails,
    CurrencyEntity currencyEntity
  ) {
    String ticker = currencyEntity.getTicker();
    String address = CryptoUtil.deriveAddress(userDetails, ticker);

    // Handing out an address is the moment it can start receiving, so it is
    // also the moment to start watching it. Waiting for the next sweep would
    // leave a new user's first deposit slower than everyone else's.
    if (nanoAddressIndex.add(ticker, address, userDetails.getUserId())) {
      nanoWebSocketService.addAccount(ticker, address);
    }

    return address;
  }

  /**
   * Turns one confirmed block into whatever it means for the bot.
   *
   * <p>A filtered subscription only ever delivers two kinds of block: one
   * published by an account the bot custodies, or a send addressed to one. The
   * first advances something already queued, the second is money arriving.
   *
   * <p>The notification carries the block itself, so nothing here needs to ask
   * the node anything: the hash, the amount and the resulting balance are all
   * in the message, and its arrival is the confirmation.
   */
  @Override
  public void handleConfirmation(String ticker, JSONObject message) {
    JSONObject block = message.optJSONObject("block");
    if (block == null) {
      // Legacy blocks arrive without contents under a filtered subscription and
      // there is nothing to route without them. The sweep still finds those.
      return;
    }

    Optional<CurrencyEntity> currencyOptional =
      currenciesService.getCurrencyByTicker(ticker);
    if (currencyOptional.isEmpty()) {
      return;
    }
    CurrencyEntity currencyEntity = currencyOptional.get();

    Optional<UserDetailsEntity> botOptional =
      coreServices.userDetailsService.getUserDetailsByUserId(botUserId);
    if (botOptional.isEmpty()) {
      fileLogger.error(
        "Bot user details not found; ignoring " + ticker + " confirmation."
      );
      return;
    }
    UserDetailsEntity botUserDetails = botOptional.get();
    String botAddress = CryptoUtil.deriveAddress(botUserDetails, ticker);

    String hash = optString(message, "hash");
    String amount = optString(message, "amount");
    String subtype = block.optString("subtype", "");
    String blockAccount = optString(block, "account");
    String balance = optString(block, "balance");
    String destination = "send".equals(subtype)
      ? optString(block, "link_as_account")
      : null;

    boolean ownAccount = nanoAddressIndex.isKnown(ticker, blockAccount);
    boolean ownDestination = nanoAddressIndex.isKnown(ticker, destination);

    // Every notification is recorded. The subscription is filtered to accounts
    // the bot holds, so this is only ever as noisy as the bot is busy, and
    // without it a deposit that push saw but declined to act on leaves no trace
    // at all - which is exactly the case that is hardest to diagnose after the
    // fact.
    fileLogger.info(
      ticker +
      " confirmation " +
      hash +
      " subtype=" +
      subtype +
      " account=" +
      blockAccount +
      (ownAccount ? " (ours)" : "") +
      " destination=" +
      destination +
      (ownDestination ? " (ours)" : "")
    );

    if (ownAccount) {
      onOwnBlockConfirmed(
        currencyEntity,
        blockAccount,
        hash,
        subtype,
        balance,
        botAddress
      );
    }

    if (ownDestination) {
      onIncomingSendConfirmed(
        currencyEntity,
        destination,
        hash,
        blockAccount,
        amount,
        botUserDetails,
        botAddress
      );
    }
  }

  /** A block the bot published reaching confirmation on the network. */
  private void onOwnBlockConfirmed(
    CurrencyEntity currencyEntity,
    String account,
    String hash,
    String subtype,
    String balance,
    String botAddress
  ) {
    boolean isHotWallet = account.equals(botAddress);

    // The block states the account's balance after it applied, so the hot
    // wallet total is already known without asking for it.
    if (
      isHotWallet &&
      balance != null &&
      !balance.equals(currencyEntity.getLiquidity())
    ) {
      currenciesService.updateChainState(
        currencyEntity.getId(),
        balance,
        null,
        null
      );
    }

    if (hash != null) {
      queuesService
        .getProcessedQueueByBlockHash(currencyEntity.getTicker(), hash)
        .ifPresent(queueEntity ->
          confirmQueuedBlock(queueEntity, currencyEntity)
        );
    }

    // The deposit now sits in the user's own account. Consolidating it into the
    // hot wallet is what the balance scan used to notice a second later.
    if (
      !isHotWallet && ("receive".equals(subtype) || "open".equals(subtype))
    ) {
      enqueueSweep(currencyEntity, account, balance, botAddress);
    }
  }

  /** Money arriving at an address the bot custodies. */
  private void onIncomingSendConfirmed(
    CurrencyEntity currencyEntity,
    String destination,
    String sendHash,
    String sender,
    String amount,
    UserDetailsEntity botUserDetails,
    String botAddress
  ) {
    UserDetailsEntity owner = destination.equals(botAddress)
      ? botUserDetails
      : loadOwner(currencyEntity.getTicker(), destination);

    enqueueDeposit(
      currencyEntity,
      owner,
      destination,
      sendHash,
      sender,
      amount,
      botUserDetails.getSeed()
    );
  }

  /**
   * Finishes a queued entry whose block the node has just confirmed, skipping
   * the block_info call that polling would otherwise need.
   */
  private void confirmQueuedBlock(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity
  ) {
    Map<String, String> commandMap = buildCommandMap();
    switch (queueEntity.getLevel()) {
      case RECEIVE -> confirmReceive(
        queueEntity,
        currencyEntity,
        commandMap,
        true
      );
      case SEND -> confirmSend(queueEntity, currencyEntity, commandMap, true);
      case UPDATE -> confirmUpdate(
        queueEntity,
        currencyEntity,
        commandMap,
        true
      );
    }
  }

  private UserDetailsEntity loadOwner(String ticker, String address) {
    String userId = nanoAddressIndex.ownerOf(ticker, address);
    if (userId == null) {
      return null;
    }
    return coreServices.userDetailsService
      .getUserDetailsByUserId(userId)
      .orElse(null);
  }

  /**
   * Queues an incoming send to be pocketed, unless it already is or already has
   * been. Shared by the websocket router and the reconciliation sweep so both
   * agree on what counts as a new deposit.
   */
  private boolean enqueueDeposit(
    CurrencyEntity currencyEntity,
    UserDetailsEntity owner,
    String destination,
    String sendHash,
    String sender,
    String amount,
    String botUserSeed
  ) {
    String ticker = currencyEntity.getTicker();
    if (owner == null || sendHash == null || amount == null) {
      fileLogger.warn(
        "Ignoring " +
        ticker +
        " deposit " +
        sendHash +
        " to " +
        destination +
        "; owner or amount could not be resolved."
      );
      return false;
    }
    if (queuesService.isDepositQueued(ticker, sendHash)) {
      fileLogger.info(
        "Skipping " + ticker + " deposit " + sendHash + "; already queued."
      );
      return false;
    }
    if (
      depositRecordsRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        sendHash,
        resolveAddressIndex(owner)
      )
    ) {
      // Already credited. Polling could not reach this state because a pocketed
      // send stops being receivable, but the node repeats confirmations for a
      // hash, so a settled deposit would otherwise be pocketed twice.
      fileLogger.info(
        "Skipping " + ticker + " deposit " + sendHash + "; already credited."
      );
      return false;
    }

    boolean isBotUser =
      owner.getUserId() != null && owner.getUserId().equals(botUserId);

    QueueDto receiveQueue = new QueueDto(
      isBotUser ? null : owner.getUserId(),
      sender,
      destination,
      LevelDto.RECEIVE,
      sendHash,
      amount,
      ticker,
      false,
      isBotUser ? botUserSeed : owner.getSeed(),
      new java.util.Date(),
      null
    );
    CryptoUtil.applySigningMaterial(receiveQueue, owner);
    queuesService.createQueue(receiveQueue);

    fileLogger.info(
      "Added transaction " +
      sendHash +
      " for account " +
      destination +
      " to the queue for ticker " +
      ticker
    );
    return true;
  }

  private void enqueueSweep(
    CurrencyEntity currencyEntity,
    String sourceAddress,
    String balance,
    String botAddress
  ) {
    enqueueSweep(
      currencyEntity,
      sourceAddress,
      balance,
      botAddress,
      loadOwner(currencyEntity.getTicker(), sourceAddress)
    );
  }

  /** Queues a user's on-chain balance to be consolidated into the hot wallet. */
  private void enqueueSweep(
    CurrencyEntity currencyEntity,
    String sourceAddress,
    String balance,
    String botAddress,
    UserDetailsEntity owner
  ) {
    String ticker = currencyEntity.getTicker();
    if (owner == null || balance == null || "0".equals(balance)) {
      return;
    }
    if (queuesService.isSweepQueued(ticker, sourceAddress)) {
      return;
    }

    fileLogger.info(
      "Adding account " +
      sourceAddress +
      " with balance " +
      balance +
      " to the queue for ticker " +
      ticker
    );

    QueueDto newQueue = new QueueDto(
      null,
      sourceAddress,
      botAddress,
      LevelDto.SEND,
      null,
      balance,
      ticker,
      false,
      owner.getSeed(),
      new java.util.Date(),
      null
    );
    CryptoUtil.applySigningMaterial(newQueue, owner);
    queuesService.createQueue(newQueue);
  }

  /**
   * Nano has no subaddresses, so the depositRecords key uses the account's HD
   * index, defaulting to the 0 that an unindexed user's address is derived
   * from.
   */
  private static Long resolveAddressIndex(UserDetailsEntity userDetails) {
    Long index = userDetails.getIndex();
    return index == null ? 0L : index;
  }

  /** org.json returns "" for a missing key, which is never a valid value here. */
  private static String optString(JSONObject json, String key) {
    String value = json.optString(key, null);
    return (value == null || value.isEmpty()) ? null : value;
  }

  private boolean isBlockConfirmed(
    CurrencyEntity currencyEntity,
    String hash
  ) {
    if (hash == null) {
      return false;
    }
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
        .put("hash", hash)
    );
    return (
      response != null &&
      Boolean.parseBoolean(response.optString(CONFIRMED_RESPONSE_KEY, "false"))
    );
  }

  /**
   * Reserves a deposit immediately before it is credited, returning empty when
   * it has already been credited once.
   *
   * <p>Polling never needed this: a pocketed send stops being receivable, so
   * the node itself would not offer it twice. Push has no such property - the
   * node repeats confirmations, and a reconnect replays whatever it decides to
   * - so the unique index on the record becomes the thing that makes crediting
   * happen exactly once. Two racing threads both find nothing and both proceed;
   * only one insert survives.
   */
  private Optional<DepositRecordEntity> claimDeposit(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity
  ) {
    String ticker = currencyEntity.getTicker();
    String depositHash = queueEntity.resolveDepositHash();
    if (depositHash == null) {
      // Nothing identifies this deposit, so nothing can stop it being credited
      // twice. Refusing is the recoverable direction.
      fileLogger.error(
        "Refusing to credit " +
        ticker +
        " queue #" +
        queueEntity.getId() +
        " with no block hash to de-duplicate on."
      );
      return Optional.empty();
    }

    DepositRecordEntity record = new DepositRecordEntity(
      ticker,
      depositHash,
      queueEntity.getIndex() == null ? 0L : queueEntity.getIndex(),
      queueEntity.getUserId(),
      queueEntity.getRaw(),
      null
    );
    record.setId(new ObjectId().toHexString());

    try {
      return Optional.of(depositRecordsRepository.insert(record));
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Ignoring " + ticker + " deposit " + depositHash + "; already credited."
      );
      return Optional.empty();
    }
  }

  public void processAddressBatch(
    Map<String, UserDetailsEntity> addressMap,
    CurrencyEntity currency,
    String botAddress,
    String botUserSeed
  ) {
    List<String> newAddressBatch = new ArrayList<>(addressMap.keySet());
    String accountsBalancesAction = "accounts_balances";
    String accountsReceivableAction = "accounts_receivable";
    sendRequestToNode(
      addressMap,
      botAddress,
      botUserSeed,
      currency,
      accountsBalancesAction,
      new JSONObject()
        .put(ACTION_COMMAND, accountsBalancesAction)
        .put("accounts", new JSONArray(newAddressBatch))
    );
    sendRequestToNode(
      addressMap,
      botAddress,
      botUserSeed,
      currency,
      accountsReceivableAction,
      new JSONObject()
        .put(ACTION_COMMAND, accountsReceivableAction)
        .put("accounts", new JSONArray(newAddressBatch))
    );
  }

  public JSONObject sendRequestToWorkServer(
    String workServerUrl,
    JSONObject requestBody
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(workServerUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build();

      CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
      );

      HttpResponse<String> httpWorkResponse = responseFuture.join();

      return new JSONObject(httpWorkResponse.body());
    } catch (Exception e) {
      fileLogger.error(
        "Error executing work server request: " + e.getMessage()
      );
      return null;
    }
  }

  public boolean sendRequestToNode(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    String botUserSeed,
    CurrencyEntity currency,
    String action,
    JSONObject requestBody
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(currency.getNodeUrl()))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build();

      CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
      );
      HttpResponse<String> response = responseFuture.get();
      return handleResponse(
        addressMap,
        botAddress,
        botUserSeed,
        currency,
        action,
        response
      );
    } catch (JSONException e) {
      fileLogger.error("Error creating JSON request: " + e.getMessage());
      return false;
    } catch (InterruptedException | ExecutionException e) {
      fileLogger.error("Error executing node request: " + e.getMessage());
      return false;
    }
  }

  public JSONObject getResponseFromNode(
    String nodeUrl,
    JSONObject requestBody
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(nodeUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build();

      CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
      );
      HttpResponse<String> response = responseFuture.get();
      if (response.statusCode() == 200) {
        return new JSONObject(response.body());
      } else {
        fileLogger.error(
          "Failed to get response from " +
          nodeUrl +
          " node. Status code: " +
          response.statusCode()
        );
        return null;
      }
    } catch (InterruptedException | ExecutionException e) {
      fileLogger.error("Error executing node request: " + e.getMessage());
      return null;
    }
  }

  public boolean handleResponse(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    String botUserSeed,
    CurrencyEntity currency,
    String action,
    HttpResponse<String> response
  ) {
    if (response.statusCode() == 200) {
      String responseData = response.body();
      try {
        JSONObject responseJson = new JSONObject(responseData);
        if ("accounts_balances".equals(action)) {
          processBalances(addressMap, botAddress, currency, responseJson);
          return true;
        } else if ("accounts_receivable".equals(action)) {
          processReceivable(addressMap, botUserSeed, currency, responseJson);
          return true;
        } else if (BLOCK_COUNT_ACTION.equals(action)) {
          JSONObject telemetryJson = getResponseFromNode(
            currency.getNodeUrl(),
            new JSONObject()
              .put(ACTION_COMMAND, TELEMETRY_ACTION)
              .put("raw", true)
          );
          return processBlockCount(responseJson, telemetryJson);
        }
      } catch (JSONException e) {
        fileLogger.error(
          "Failed to parse response JSON for " + action + ": " + e.getMessage()
        );
        return false;
      }
    } else {
      fileLogger.error(
        "Failed to get response from " +
        currency.getNodeUrl() +
        " node. Status code: " +
        response.statusCode()
      );
    }
    return false;
  }

  public boolean processBlockCount(
    JSONObject responseJson,
    JSONObject telemetryJson
  ) {
    try {
      long localCount = responseJson.getLong("count");
      long localCemented = responseJson.getLong("cemented");

      if (telemetryJson == null || !telemetryJson.has("metrics")) {
        fileLogger.warn(
          "Node telemetry is unavailable; cannot verify sync against peers"
        );
        return false;
      }

      JSONArray metrics = telemetryJson.getJSONArray("metrics");
      List<Long> peerCounts = new ArrayList<>();
      List<Long> peerCemented = new ArrayList<>();
      for (int i = 0; i < metrics.length(); i++) {
        JSONObject metric = metrics.getJSONObject(i);
        if (!metric.has("block_count") || !metric.has("cemented_count")) {
          continue;
        }
        long peerCount = metric.getLong("block_count");
        long peerCementedCount = metric.getLong("cemented_count");
        if (peerCount <= 0) {
          continue;
        }
        peerCounts.add(peerCount);
        peerCemented.add(peerCementedCount);
      }

      if (peerCounts.size() < MIN_TELEMETRY_PEERS) {
        fileLogger.warn(
          "Not enough peer telemetry samples to verify sync: " +
          peerCounts.size()
        );
        return false;
      }

      long medianCount = median(peerCounts);
      long medianCemented = median(peerCemented);
      long countLag = medianCount - localCount;
      long cementedLag = medianCemented - localCemented;

      if (countLag > MAX_TELEMETRY_LAG || cementedLag > MAX_TELEMETRY_LAG) {
        fileLogger.warn(
          "Node is falling behind peers. Block count lag: " +
          countLag +
          ", cemented lag: " +
          cementedLag +
          " (peer median count=" +
          medianCount +
          ", cemented=" +
          medianCemented +
          ")"
        );
        return false;
      }
      return true;
    } catch (JSONException e) {
      fileLogger.error(
        "Failed to parse block count or telemetry JSON: " + e.getMessage()
      );
      return false;
    }
  }

  static long median(List<Long> values) {
    List<Long> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int size = sorted.size();
    if (size % 2 == 1) {
      return sorted.get(size / 2);
    }
    return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
        Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private byte[] resolveSigningKey(QueueEntity queueEntity) {
    return hexStringToByteArray(
      CryptoUtil.resolvePrivateKey(queueEntity).toString()
    );
  }

  public static byte[] derivePrivateKey(byte[] seed, int index) {
    byte[] indexBytes = ByteBuffer.allocate(4).putInt(index).array(); // big-endian
    byte[] data = new byte[seed.length + 4];
    System.arraycopy(seed, 0, data, 0, seed.length);
    System.arraycopy(indexBytes, 0, data, seed.length, 4);
    Blake2bDigest digest = new Blake2bDigest(256); // 256-bit output
    digest.update(data, 0, data.length);
    byte[] privateKey = new byte[32];
    digest.doFinal(privateKey, 0);
    return privateKey;
  }

  public void processBalances(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    CurrencyEntity currencyEntity,
    JSONObject responseJson
  ) {
    try {
      JSONObject balances = responseJson.optJSONObject("balances");
      if (balances == null) {
        return;
      }

      for (Iterator<String> accounts = balances.keys(); accounts.hasNext();) {
        String account = accounts.next();
        String balance = balances
          .getJSONObject(account)
          .getString(BALANCE_RESPONSE_KEY);

        if (botAddress.equals(account)) {
          if (!balance.equals(currencyEntity.getLiquidity())) {
            currencyEntity.setLiquidity(balance);
            currenciesService.updateChainState(
              currencyEntity.getId(),
              balance,
              null,
              null
            );
          }
          continue;
        }

        enqueueSweep(
          currencyEntity,
          account,
          balance,
          botAddress,
          addressMap.get(account)
        );
      }
    } catch (JSONException e) {
      fileLogger.error("Failed to parse balances JSON: " + e.getMessage());
    }
  }

  public void processReceivable(
    Map<String, UserDetailsEntity> addressMap,
    String botUserSeed,
    CurrencyEntity currencyEntity,
    JSONObject responseJson
  ) {
    String ticker = currencyEntity.getTicker();
    try {
      // The node serializes an empty result as "" rather than {}, so a batch
      // where nobody has anything receivable - which is almost every batch -
      // arrives as a string. Reading it as an object threw on each one, and the
      // resulting error per batch per second was the bulk of this log.
      JSONObject blocks = responseJson.optJSONObject("blocks");
      if (blocks == null) {
        return;
      }

      for (Iterator<String> accounts = blocks.keys(); accounts.hasNext();) {
        String account = accounts.next();
        UserDetailsEntity owner = addressMap.get(account);
        // An individual account with nothing receivable is empty the same way.
        JSONArray accountHashes = blocks.optJSONArray(account);
        if (owner == null || accountHashes == null) {
          continue;
        }

        for (int i = 0; i < accountHashes.length(); i++) {
          String hash = accountHashes.getString(i);
          if (queuesService.isDepositQueued(ticker, hash)) {
            continue;
          }

          JSONObject response = getResponseFromNode(
            currencyEntity.getNodeUrl(),
            new JSONObject()
              .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
              .put("hash", hash)
          );

          if (
            response == null ||
            !Boolean.parseBoolean(
              response.optString(CONFIRMED_RESPONSE_KEY, "false")
            )
          ) {
            continue;
          }

          fileLogger.info(
            "Receivable transaction has been confirmed on the network, adding to queue."
          );

          enqueueDeposit(
            currencyEntity,
            owner,
            account,
            hash,
            optString(response, "block_account"),
            optString(response, "amount"),
            botUserSeed
          );
        }
      }
    } catch (JSONException e) {
      fileLogger.error("Failed to parse receivable JSON: " + e.getMessage());
    }
  }
}
