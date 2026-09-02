package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Drives the service through a mocked {@link HttpClient}, capturing the
 * {@link WebSocket.Listener} it hands to the builder so incoming frames can be
 * delivered directly. That covers everything short of the socket itself, which
 * is the part the JDK is responsible for.
 */
class NanoWebSocketServiceTest {

  private static final String TICKER = "XNO";
  private static final String URL = "ws://node:7078";

  private NanoAddressIndex addressIndex;
  private NanoConfirmationHandler handler;
  private HttpClient client;
  private WebSocket socket;
  private NanoWebSocketService service;
  private ArgumentCaptor<WebSocket.Listener> listenerCaptor;
  private List<String> sent;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws Exception {
    addressIndex = new NanoAddressIndex();
    handler = mock(NanoConfirmationHandler.class);

    ObjectProvider<NanoConfirmationHandler> handlerProvider = mock(
      ObjectProvider.class
    );
    when(handlerProvider.getIfAvailable()).thenReturn(handler);

    sent = new ArrayList<>();
    socket = mock(WebSocket.class);
    when(socket.sendText(any(), anyBoolean())).thenAnswer(invocation -> {
      sent.add(invocation.getArgument(0).toString());
      return CompletableFuture.completedFuture(socket);
    });

    WebSocket.Builder builder = mock(
      WebSocket.Builder.class,
      RETURNS_SELF
    );
    listenerCaptor = ArgumentCaptor.forClass(WebSocket.Listener.class);
    when(builder.buildAsync(any(), listenerCaptor.capture()))
      .thenReturn(CompletableFuture.completedFuture(socket));

    client = mock(HttpClient.class);
    when(client.newWebSocketBuilder()).thenReturn(builder);

    service = new NanoWebSocketService(addressIndex, handlerProvider, client);
  }

  @AfterEach
  void tearDown() throws Exception {
    service.shutdown();
  }

  private static CurrencyEntity currency(String url) {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker(TICKER);
    entity.setWebsocketUrl(url);
    return entity;
  }

  private void indexAddresses(int count) {
    for (int i = 0; i < count; i++) {
      addressIndex.add(TICKER, "nano_" + i, "user-" + i);
    }
  }

  /** Messages the node was sent whose action matches, parsed back out. */
  private List<JSONObject> sentWithAction(String action) {
    return sent
      .stream()
      .map(NanoWebSocketServiceTest::parse)
      .filter(message -> action.equals(message.optString("action")))
      .collect(Collectors.toList());
  }

  private static JSONObject parse(String text) {
    try {
      return new JSONObject(text);
    } catch (JSONException e) {
      throw new AssertionError("Sent a non-JSON message: " + text, e);
    }
  }

  // ------------------------------------------------------------ connecting

  @Test
  void shouldSubscribeWithTheKnownAccounts() throws Exception {
    indexAddresses(2);

    service.ensureSubscribed(currency(URL));

    List<JSONObject> subscribes = sentWithAction("subscribe");
    assertEquals(1, subscribes.size());
    assertEquals("confirmation", subscribes.get(0).getString("topic"));
    assertEquals(
      2,
      subscribes.get(0).getJSONObject("options").getJSONArray("accounts").length()
    );
    assertTrue(service.isSubscribed(TICKER));
  }

  /**
   * The socket was down for some window before this point, so anything
   * confirmed in it was never announced and only a sweep will find it.
   */
  @Test
  void shouldRequestReconciliationOnSubscribe() throws Exception {
    indexAddresses(1);

    service.ensureSubscribed(currency(URL));

    verify(handler).requestReconciliation(TICKER);
  }

  /** An empty filter is not a narrow subscription, it is every confirmation on the network. */
  @Test
  void shouldNotSubscribeBeforeTheAddressIndexIsBuilt() throws Exception {
    service.ensureSubscribed(currency(URL));

    assertTrue(sent.isEmpty());
    assertFalse(service.isSubscribed(TICKER));
    verify(client, never()).newWebSocketBuilder();
  }

  @Test
  void shouldIgnoreACurrencyWithNoWebsocketUrl() throws Exception {
    indexAddresses(1);

    service.ensureSubscribed(currency(null));
    service.ensureSubscribed(currency("   "));

    assertTrue(sent.isEmpty());
    assertFalse(service.isSubscribed(TICKER));
  }

  @Test
  void shouldRefuseANonWebsocketUrl() throws Exception {
    indexAddresses(1);

    service.ensureSubscribed(currency("http://node:7078"));

    assertTrue(sent.isEmpty());
    assertFalse(service.isSubscribed(TICKER));
  }

  /**
   * A node is under no obligation to accept a frame carrying tens of thousands
   * of accounts, so the filter is built up in pieces.
   */
  @Test
  void shouldChunkALargeAccountList() throws Exception {
    indexAddresses(2500);

    service.ensureSubscribed(currency(URL));

    List<JSONObject> subscribes = sentWithAction("subscribe");
    List<JSONObject> updates = sentWithAction("update");

    assertEquals(
      1000,
      subscribes.get(0).getJSONObject("options").getJSONArray("accounts").length()
    );

    int added = 0;
    for (JSONObject update : updates) {
      added +=
        update.getJSONObject("options").getJSONArray("accounts_add").length();
    }
    assertEquals(1500, added);
  }

  // ------------------------------------------------------------- accounts

  @Test
  void shouldPushANewlyIssuedAddressWithoutWaitingForASweep() throws Exception {
    indexAddresses(1);
    service.ensureSubscribed(currency(URL));
    sent.clear();

    service.addAccount(TICKER, "nano_new");

    List<JSONObject> updates = sentWithAction("update");
    assertEquals(1, updates.size());
    assertEquals(
      "nano_new",
      updates
        .get(0)
        .getJSONObject("options")
        .getJSONArray("accounts_add")
        .getString(0)
    );
  }

  @Test
  void shouldNotResendAccountsThatAreAlreadySubscribed() throws Exception {
    indexAddresses(2);
    service.ensureSubscribed(currency(URL));
    sent.clear();

    service.ensureSubscribed(currency(URL));

    assertTrue(sentWithAction("update").isEmpty());
  }

  /**
   * Equal sizes do not mean equal sets. A rebuild that drops one address and
   * adds another leaves the count unchanged, and comparing only sizes left the
   * new address off the node's filter entirely.
   */
  @Test
  void shouldSyncAnAddressThatReplacedAnotherOfTheSameCount() throws Exception {
    indexAddresses(2);
    service.ensureSubscribed(currency(URL));
    sent.clear();

    addressIndex.clear();
    addressIndex.add(TICKER, "nano_0", "user-0");
    addressIndex.add(TICKER, "nano_replacement", "user-9");

    service.ensureSubscribed(currency(URL));

    List<JSONObject> updates = sentWithAction("update");
    assertEquals(1, updates.size());
    var added = updates
      .get(0)
      .getJSONObject("options")
      .getJSONArray("accounts_add");
    assertEquals(1, added.length());
    assertEquals("nano_replacement", added.getString(0));
  }

  // ------------------------------------------------------------- messages

  private WebSocket.Listener connectedListener() {
    indexAddresses(1);
    service.ensureSubscribed(currency(URL));
    return listenerCaptor.getValue();
  }

  private static JSONObject confirmation(String hash) throws JSONException {
    return new JSONObject()
      .put("topic", "confirmation")
      .put(
        "message",
        new JSONObject()
          .put("hash", hash)
          .put("block", new JSONObject().put("subtype", "send"))
      );
  }

  @Test
  void shouldHandOffAConfirmation() throws Exception {
    WebSocket.Listener listener = connectedListener();

    listener.onText(socket, confirmation("HASH1").toString(), true);

    verify(handler, timeout(2000)).handleConfirmation(eq(TICKER), any());
  }

  /** A frame can arrive in pieces, and only the whole of it is valid JSON. */
  @Test
  void shouldReassembleAFragmentedFrame() throws Exception {
    WebSocket.Listener listener = connectedListener();
    String text = confirmation("HASH1").toString();
    int split = text.length() / 2;

    listener.onText(socket, text.substring(0, split), false);
    listener.onText(socket, text.substring(split), true);

    verify(handler, timeout(2000)).handleConfirmation(eq(TICKER), any());
  }

  @Test
  void shouldIgnoreTopicsOtherThanConfirmation() throws Exception {
    WebSocket.Listener listener = connectedListener();

    listener.onText(
      socket,
      new JSONObject()
        .put("topic", "vote")
        .put("message", new JSONObject())
        .toString(),
      true
    );

    Thread.sleep(200);
    verify(handler, never()).handleConfirmation(any(), any());
  }

  /** One malformed frame must not take the listener down with it. */
  @Test
  void shouldSurviveAMalformedFrameAndKeepWorking() throws Exception {
    WebSocket.Listener listener = connectedListener();

    listener.onText(socket, "{not json", true);
    listener.onText(socket, confirmation("HASH1").toString(), true);

    verify(handler, timeout(2000)).handleConfirmation(eq(TICKER), any());
  }

  @Test
  void shouldIgnoreAConfirmationWithNoMessage() throws Exception {
    WebSocket.Listener listener = connectedListener();

    listener.onText(
      socket,
      new JSONObject().put("topic", "confirmation").toString(),
      true
    );

    Thread.sleep(200);
    verify(handler, never()).handleConfirmation(any(), any());
  }

  // ------------------------------------------------------------- lifecycle

  @Test
  void closingTheSocketShouldDropTheSubscription() throws Exception {
    WebSocket.Listener listener = connectedListener();
    assertTrue(service.isSubscribed(TICKER));

    listener.onClose(socket, 1006, "gone");

    assertFalse(service.isSubscribed(TICKER));
    verify(socket).abort();
  }

  @Test
  void disconnectShouldAbortAndForget() throws Exception {
    indexAddresses(1);
    service.ensureSubscribed(currency(URL));

    service.disconnect(TICKER);

    assertFalse(service.isSubscribed(TICKER));
    verify(socket).abort();
  }

  @Test
  void removingTheUrlShouldDisconnect() throws Exception {
    indexAddresses(1);
    service.ensureSubscribed(currency(URL));

    service.ensureSubscribed(currency(null));

    assertFalse(service.isSubscribed(TICKER));
    verify(socket).abort();
  }

  @Test
  void shutdownShouldCloseEverythingAndStopReconnecting() throws Exception {
    indexAddresses(1);
    service.ensureSubscribed(currency(URL));

    service.shutdown();
    assertFalse(service.isSubscribed(TICKER));
    verify(socket).abort();

    sent.clear();
    service.ensureSubscribed(currency(URL));
    assertTrue(sent.isEmpty());
  }

  /** A changed URL has to land on the new node, not keep talking to the old one. */
  @Test
  void changingTheUrlShouldReconnect() throws Exception {
    indexAddresses(1);
    service.ensureSubscribed(currency(URL));

    service.ensureSubscribed(currency("ws://other-node:7078"));

    verify(socket).abort();
    assertEquals(2, sentWithAction("subscribe").size());
  }
}
