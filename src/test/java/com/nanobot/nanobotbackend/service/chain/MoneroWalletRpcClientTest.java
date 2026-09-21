package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoneroWalletRpcClientTest {

  private MockWebServer server;
  private MoneroWalletRpcClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    client = new MoneroWalletRpcClient();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  /** monerod's endpoint takes no credentials, so no digest round trip. */
  private CurrencyEntity currency() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("XMR");
    currency.setNodeUrl(server.url("/json_rpc").toString());
    currency.setWalletRpcUrl(server.url("/json_rpc").toString());
    return currency;
  }

  @Test
  void successfulCallsShouldReturnTheResultObject() throws Exception {
    server.enqueue(
      new MockResponse().setBody(
        "{\"id\":\"0\",\"jsonrpc\":\"2.0\",\"result\":{\"fee\":71860000}}"
      )
    );

    RpcResponse response = client.walletDetailed(
      currency(),
      "transfer",
      new JSONObject()
    );

    assertTrue(response.isSuccess());
    assertEquals(71860000L, response.result().getLong("fee"));
    assertFalse(response.refused());
  }

  /**
   * The one distinction the withdrawal path needs: -37 is a wallet saying
   * "wait", and it has to arrive as a refusal with its code intact rather than
   * as a bare null.
   */
  @Test
  void refusalsShouldKeepTheCodeAndMessage() {
    server.enqueue(
      new MockResponse().setBody(
        "{\"id\":\"0\",\"jsonrpc\":\"2.0\",\"error\":{\"code\":-37," +
        "\"message\":\"not enough unlocked money\"}}"
      )
    );

    RpcResponse response = client.walletDetailed(
      currency(),
      "transfer",
      new JSONObject()
    );

    assertFalse(response.isSuccess());
    assertTrue(response.refused());
    assertFalse(response.isTransportFailure());
    assertEquals(-37, response.errorCode());
    assertEquals("not enough unlocked money", response.errorMessage());
  }

  @Test
  void plainCallsShouldStillCollapseRefusalsToNull() {
    server.enqueue(
      new MockResponse().setBody(
        "{\"id\":\"0\",\"jsonrpc\":\"2.0\",\"error\":{\"code\":-17," +
        "\"message\":\"not enough money\"}}"
      )
    );

    assertNull(client.wallet(currency(), "transfer", new JSONObject()));
  }

  @Test
  void httpErrorsShouldBeTransportFailures() {
    server.enqueue(new MockResponse().setResponseCode(503));

    RpcResponse response = client.callDetailed(
      currency().getNodeUrl(),
      null,
      null,
      "get_info",
      null
    );

    assertTrue(response.isTransportFailure());
    assertFalse(response.refused());
    assertNull(response.errorCode());
  }

  @Test
  void aWalletThatIsDownShouldBeATransportFailure() throws Exception {
    CurrencyEntity currency = currency();
    server.shutdown();

    RpcResponse response = client.walletDetailed(
      currency,
      "transfer",
      new JSONObject()
    );

    assertTrue(response.isTransportFailure());
    assertFalse(response.refused());
  }

  @Test
  void missingUrlShouldBeATransportFailure() {
    CurrencyEntity currency = currency();
    currency.setWalletRpcUrl(null);

    RpcResponse response = client.walletDetailed(
      currency,
      "transfer",
      new JSONObject()
    );

    assertTrue(response.isTransportFailure());
  }
}
