package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Confirmation notifications from Nano-family nodes, one connection per
 * currency.
 *
 * <p>Replaces asking the node "has anything happened" every second for every
 * user with the node telling us when something has. The subscription is
 * filtered server side to the accounts the bot custodies, so the node sends
 * nothing at all while the bot is idle.
 *
 * <p>Delivery is explicitly not guaranteed - the node drops notifications under
 * load and a dead socket can go unnoticed until the next write - so this is
 * only ever an accelerator. Correctness still rests on the periodic
 * reconciliation sweep, which is why every successful subscription asks for one
 * rather than assuming the socket saw everything while it was gone.
 */
@Service
public class NanoWebSocketService {

  private static final String TOPIC_CONFIRMATION = "confirmation";
  private static final String ACTION = "action";
  private static final String TOPIC = "topic";
  private static final String OPTIONS = "options";

  /**
   * Accounts per message. The whole set in one frame would be a few hundred
   * kilobytes, and the node is under no obligation to accept a frame that
   * large, so the filter is built up in pieces instead.
   */
  private static final int ACCOUNT_CHUNK_SIZE = 1000;

  private static final long PING_INTERVAL_MILLIS = 30_000L;

  /**
   * A filtered subscription on an idle bot is silent, so pong replies are the
   * only proof the socket is alive. Three missed pings is treated as dead.
   */
  private static final long SILENCE_TIMEOUT_MILLIS = 100_000L;

  private static final long MIN_BACKOFF_MILLIS = 1_000L;
  private static final long MAX_BACKOFF_MILLIS = 60_000L;

  /**
   * Bounded so a burst cannot grow the heap without limit. Anything dropped
   * here is recovered by the reconciliation sweep.
   */
  private static final int WORKER_QUEUE_CAPACITY = 20_000;

  private final NanoAddressIndex addressIndex;
  private final ObjectProvider<NanoConfirmationHandler> handlerProvider;
  private final FileLogger fileLogger;
  private final HttpClient client;
  private final ScheduledExecutorService scheduler;
  private final Map<String, NodeConnection> connections =
    new ConcurrentHashMap<>();

  private volatile boolean shuttingDown;
  private volatile NanoConfirmationHandler handler;

  @Autowired
  public NanoWebSocketService(
    NanoAddressIndex addressIndex,
    ObjectProvider<NanoConfirmationHandler> handlerProvider
  ) {
    this(addressIndex, handlerProvider, HttpClient.newHttpClient());
  }

  /** Constructor for testing: allows injecting a client. */
  NanoWebSocketService(
    NanoAddressIndex addressIndex,
    ObjectProvider<NanoConfirmationHandler> handlerProvider,
    HttpClient client
  ) {
    this.addressIndex = addressIndex;
    this.handlerProvider = handlerProvider;
    this.client = client;
    this.fileLogger = new FileLogger("NanoWebSocketService");
    this.scheduler =
      Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "nano-ws-keepalive");
        thread.setDaemon(true);
        return thread;
      });
    this.scheduler.scheduleWithFixedDelay(
        this::keepAlive,
        PING_INTERVAL_MILLIS,
        PING_INTERVAL_MILLIS,
        TimeUnit.MILLISECONDS
      );
  }

  /**
   * Brings the connection for a currency in line with its configuration. Safe
   * to call on every cron tick; it does nothing once connected and current.
   *
   * <p>A currency with no websocket URL is left entirely alone, which is what
   * keeps this opt-in per currency.
   */
  public void ensureSubscribed(CurrencyEntity currencyEntity) {
    if (shuttingDown || currencyEntity == null) {
      return;
    }
    String ticker = currencyEntity.getTicker();
    String url = currencyEntity.getWebsocketUrl();

    if (url == null || url.isBlank()) {
      disconnect(ticker);
      return;
    }

    // Subscribing before the address index is built would register an empty
    // filter, and an empty filter is silent. Waiting means the first
    // reconciliation sweep always runs before push takes over.
    if (!addressIndex.isPopulated(ticker)) {
      return;
    }

    NodeConnection connection = connections.compute(ticker, (key, existing) -> {
      if (existing != null && existing.url.equals(url)) {
        return existing;
      }
      if (existing != null) {
        fileLogger.info(
          key + " websocket URL changed, reconnecting to " + url
        );
        existing.shutdown();
      }
      return new NodeConnection(key, url);
    });

    connection.ensureConnected();
    connection.syncAccounts();
  }

  /** Whether push notifications are currently believed to be flowing. */
  public boolean isSubscribed(String ticker) {
    NodeConnection connection = connections.get(ticker);
    return connection != null && connection.subscribed;
  }

  /**
   * Starts watching a newly handed out deposit address without waiting for the
   * next sweep, so a user's first deposit is not slower than any other.
   */
  public void addAccount(String ticker, String address) {
    NodeConnection connection = connections.get(ticker);
    if (connection != null) {
      connection.addAccounts(List.of(address));
    }
  }

  public void disconnect(String ticker) {
    NodeConnection connection = connections.remove(ticker);
    if (connection != null) {
      connection.shutdown();
    }
  }

  @PreDestroy
  public void shutdown() {
    shuttingDown = true;
    scheduler.shutdownNow();
    connections.values().forEach(NodeConnection::shutdown);
    connections.clear();
  }

  /**
   * The handler is pulled through a provider because it is the Nano adapter,
   * which depends on this service in turn. Resolving it lazily breaks that
   * cycle, but it also means a wiring failure would otherwise show up as
   * notifications quietly going nowhere, so an unresolvable handler is logged
   * rather than treated as "nothing to do".
   */
  private NanoConfirmationHandler handler() {
    NanoConfirmationHandler resolved = handler;
    if (resolved != null) {
      return resolved;
    }
    resolved = handlerProvider.getIfAvailable();
    if (resolved == null) {
      fileLogger.error(
        "No NanoConfirmationHandler is available; confirmations cannot be" +
        " acted on and deposits will only be found by the reconciliation sweep."
      );
      return null;
    }
    handler = resolved;
    return resolved;
  }

  private void keepAlive() {
    if (shuttingDown) {
      return;
    }
    for (NodeConnection connection : connections.values()) {
      try {
        connection.keepAlive();
      } catch (Exception e) {
        fileLogger.error(
          "Keepalive failed for " + connection.ticker + ": " + e.getMessage()
        );
      }
    }
  }

  /** One node's socket, its subscription state and its reconnect schedule. */
  private final class NodeConnection {

    private final String ticker;
    private final String url;
    private final StringBuilder partial = new StringBuilder();
    private final Set<String> subscribedAccounts = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor worker;

    private volatile WebSocket webSocket;
    private volatile boolean connecting;
    private volatile boolean subscribed;
    private volatile long lastMessageMillis;
    private volatile long nextAttemptMillis;
    private volatile int failures;

    private CompletableFuture<?> sendChain = CompletableFuture.completedFuture(
      null
    );

    private NodeConnection(String ticker, String url) {
      this.ticker = ticker;
      this.url = url;
      this.worker =
        new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(WORKER_QUEUE_CAPACITY),
          runnable -> {
            Thread thread = new Thread(runnable, "nano-ws-" + ticker);
            thread.setDaemon(true);
            return thread;
          }
        );
    }

    /**
     * Synchronized because the keepalive thread and the cron thread both call
     * this. Testing the guard and setting {@code connecting} are separate steps,
     * so without the lock both can pass and open a second socket for the same
     * currency, and the two listeners then share one reassembly buffer.
     *
     * <p>Holds the lock only long enough to start the connection; buildAsync
     * returns immediately and the rest happens on the callback.
     */
    private synchronized void ensureConnected() {
      if (webSocket != null || connecting || shuttingDown) {
        return;
      }
      if (System.currentTimeMillis() < nextAttemptMillis) {
        return;
      }

      URI uri;
      try {
        uri = URI.create(url);
      } catch (IllegalArgumentException e) {
        fileLogger.error(
          "Invalid " + ticker + " websocket URL " + url + ": " + e.getMessage()
        );
        backOff();
        return;
      }
      String scheme = uri.getScheme();
      if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
        fileLogger.error(
          "Ignoring " + ticker + " websocket URL " + url + "; expected ws://"
        );
        backOff();
        return;
      }

      connecting = true;
      fileLogger.info("Connecting to " + ticker + " websocket at " + url);
      client
        .newWebSocketBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .buildAsync(uri, new ConfirmationListener(this))
        .whenComplete((socket, error) -> {
          connecting = false;
          if (error != null) {
            fileLogger.warn(
              "Could not connect to " +
              ticker +
              " websocket: " +
              error.getMessage()
            );
            backOff();
            return;
          }
          // Shutdown can land while a connection is still being established.
          // Adopting the socket here would reopen work on a connection that has
          // already been torn down.
          if (shuttingDown) {
            socket.abort();
            return;
          }
          webSocket = socket;
          lastMessageMillis = System.currentTimeMillis();
          failures = 0;
          subscribe();
        });
    }

    private void subscribe() {
      List<String> accounts = addressIndex.addresses(ticker);
      if (accounts.isEmpty()) {
        // Subscribing without accounts would ask for every confirmation on the
        // network, which is emphatically not wanted.
        fileLogger.warn(
          "No known " + ticker + " addresses; not subscribing yet"
        );
        return;
      }

      subscribedAccounts.clear();

      List<String> first = accounts.subList(
        0,
        Math.min(ACCOUNT_CHUNK_SIZE, accounts.size())
      );
      send(
        new JSONObject()
          .put(ACTION, "subscribe")
          .put(TOPIC, TOPIC_CONFIRMATION)
          .put("ack", true)
          .put("id", ticker)
          .put(
            OPTIONS,
            new JSONObject()
              .put("accounts", new JSONArray(first))
              .put("include_election_info", "false")
          )
      );
      subscribedAccounts.addAll(first);

      if (accounts.size() > first.size()) {
        addAccounts(accounts.subList(first.size(), accounts.size()));
      }

      subscribed = true;
      fileLogger.info(
        "Subscribed to " +
        ticker +
        " confirmations for " +
        accounts.size() +
        " accounts"
      );

      // The socket was down, or never up, for some window before now. Anything
      // confirmed in that window was never announced, so a sweep is the only
      // thing that finds it.
      NanoConfirmationHandler target = handler();
      if (target != null) {
        target.requestReconciliation(ticker);
      }
    }

    /** Extends the live filter, skipping accounts the node already has. */
    private void addAccounts(List<String> accounts) {
      if (webSocket == null) {
        return;
      }
      List<String> pending = new ArrayList<>();
      for (String account : accounts) {
        if (account != null && subscribedAccounts.add(account)) {
          pending.add(account);
        }
      }
      for (int i = 0; i < pending.size(); i += ACCOUNT_CHUNK_SIZE) {
        List<String> chunk = pending.subList(
          i,
          Math.min(i + ACCOUNT_CHUNK_SIZE, pending.size())
        );
        send(
          new JSONObject()
            .put(ACTION, "update")
            .put(TOPIC, TOPIC_CONFIRMATION)
            .put(
              OPTIONS,
              new JSONObject().put("accounts_add", new JSONArray(chunk))
            )
        );
      }
    }

    /**
     * Picks up addresses that appeared since the subscription was built.
     *
     * <p>Compares membership rather than size. Equal sizes do not mean equal
     * sets: a rebuild that drops one address and adds another leaves the count
     * unchanged, and a size check would then never send the new one, leaving
     * that user's deposits invisible to push until the next resubscribe.
     */
    private void syncAccounts() {
      if (!subscribed || webSocket == null) {
        return;
      }
      List<String> missing = addressIndex
        .addresses(ticker)
        .stream()
        .filter(address -> !subscribedAccounts.contains(address))
        .collect(Collectors.toList());
      if (missing.isEmpty()) {
        return;
      }
      addAccounts(missing);
    }

    private void keepAlive() {
      if (webSocket == null) {
        ensureConnected();
        return;
      }
      if (
        System.currentTimeMillis() - lastMessageMillis > SILENCE_TIMEOUT_MILLIS
      ) {
        fileLogger.warn(
          ticker + " websocket stopped answering pings, reconnecting"
        );
        reconnect();
        return;
      }
      send(new JSONObject().put(ACTION, "ping"));
    }

    /**
     * Sends are chained because the JDK client rejects a second sendText while
     * the first is still in flight, and a failed send must not stall the ones
     * behind it.
     */
    private void send(JSONObject payload) {
      WebSocket socket = webSocket;
      if (socket == null) {
        return;
      }
      String text = payload.toString();
      synchronized (this) {
        sendChain =
          sendChain
            .handle((ignored, error) -> null)
            .thenCompose(ignored -> socket.sendText(text, true))
            .exceptionally(error -> {
              fileLogger.warn(
                "Failed to send on " + ticker + " websocket: " +
                error.getMessage()
              );
              reconnect();
              return null;
            });
      }
    }

    private void onMessage(String text) {
      lastMessageMillis = System.currentTimeMillis();
      try {
        worker.execute(() -> dispatch(text));
      } catch (RejectedExecutionException e) {
        fileLogger.error(
          ticker +
          " websocket backlog is full; dropping a notification. A" +
          " reconciliation sweep has been requested to recover it."
        );
        // Ask for the sweep rather than waiting for the periodic one. Being
        // subscribed stretches that interval to five minutes, which is a long
        // time to sit on a deposit that is known to have been dropped.
        NanoConfirmationHandler target = handler();
        if (target != null) {
          target.requestReconciliation(ticker);
        }
      }
    }

    private void dispatch(String text) {
      try {
        JSONObject envelope = new JSONObject(text);
        if (envelope.has("ack")) {
          fileLogger.info(
            ticker + " websocket ack: " + envelope.optString("ack")
          );
          return;
        }
        if (!TOPIC_CONFIRMATION.equals(envelope.optString(TOPIC))) {
          return;
        }
        JSONObject message = envelope.optJSONObject("message");
        if (message == null) {
          fileLogger.warn(
            ticker + " confirmation arrived with no message body"
          );
          return;
        }
        NanoConfirmationHandler target = handler();
        if (target != null) {
          target.handleConfirmation(ticker, message);
        }
      } catch (JSONException e) {
        fileLogger.error(
          "Unparseable " + ticker + " websocket message: " + e.getMessage()
        );
      } catch (Exception e) {
        fileLogger.error(
          "Failed handling " + ticker + " confirmation: " + e.getMessage()
        );
      }
    }

    private void reconnect() {
      WebSocket socket = webSocket;
      webSocket = null;
      subscribed = false;
      subscribedAccounts.clear();
      partial.setLength(0);
      if (socket != null) {
        socket.abort();
      }
      backOff();
    }

    private void backOff() {
      failures = Math.min(failures + 1, 16);
      long delay = Math.min(
        MAX_BACKOFF_MILLIS,
        MIN_BACKOFF_MILLIS << Math.min(failures - 1, 6)
      );
      nextAttemptMillis = System.currentTimeMillis() + delay;
    }

    private void shutdown() {
      WebSocket socket = webSocket;
      webSocket = null;
      subscribed = false;
      if (socket != null) {
        socket.abort();
      }
      worker.shutdownNow();
    }
  }

  /** Reassembles fragmented frames and hands whole messages to the worker. */
  private final class ConfirmationListener implements WebSocket.Listener {

    private final NodeConnection connection;

    private ConfirmationListener(NodeConnection connection) {
      this.connection = connection;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(
      WebSocket webSocket,
      CharSequence data,
      boolean last
    ) {
      connection.partial.append(data);
      if (last) {
        String text = connection.partial.toString();
        connection.partial.setLength(0);
        connection.onMessage(text);
      }
      webSocket.request(1);
      return null;
    }

    @Override
    public CompletionStage<?> onClose(
      WebSocket webSocket,
      int statusCode,
      String reason
    ) {
      fileLogger.warn(
        connection.ticker +
        " websocket closed (" +
        statusCode +
        " " +
        reason +
        ")"
      );
      connection.reconnect();
      return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      fileLogger.error(
        connection.ticker + " websocket error: " + error.getMessage()
      );
      connection.reconnect();
    }
  }
}
