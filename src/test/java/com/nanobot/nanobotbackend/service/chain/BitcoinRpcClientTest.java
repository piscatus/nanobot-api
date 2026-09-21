package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BitcoinRpcClientTest {

  private MockWebServer server;
  private BitcoinRpcClient client;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    client = new BitcoinRpcClient();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  private CurrencyEntity currency() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("BTC");
    currency.setNodeUrl(server.url("/").toString());
    currency.setWalletRpcUrl(server.url("/wallet/nanobot-dev").toString());
    currency.setWalletRpcUser("anon");
    currency.setWalletRpcPassword("secret");
    return currency;
  }

  @Test
  void walletCallsShouldHitTheWalletPathWithBasicAuth() throws Exception {
    server.enqueue(
      new MockResponse().setBody("{\"result\":{\"blocks\":5},\"error\":null}")
    );

    JSONObject result = client.wallet(currency(), "getwalletinfo");

    assertNotNull(result);
    assertEquals(5, result.getInt("blocks"));

    RecordedRequest request = server.takeRequest();
    assertEquals("/wallet/nanobot-dev", request.getPath());
    assertEquals(
      "Basic " +
      Base64.getEncoder()
        .encodeToString("anon:secret".getBytes(StandardCharsets.UTF_8)),
      request.getHeader("Authorization")
    );
  }

  /**
   * monerod needs no credentials, so its client deliberately omits them on
   * daemon calls. bitcoind authenticates everything, and getting this wrong
   * fails only at runtime with a 401.
   */
  @Test
  void daemonCallsShouldAlsoAuthenticate() throws Exception {
    server.enqueue(
      new MockResponse().setBody("{\"result\":{\"blocks\":9},\"error\":null}")
    );

    client.daemon(currency(), "getblockchaininfo");

    RecordedRequest request = server.takeRequest();
    assertEquals("/", request.getPath());
    assertNotNull(request.getHeader("Authorization"));
  }

  @Test
  void parametersShouldBeSentPositionally() throws Exception {
    server.enqueue(
      new MockResponse().setBody("{\"result\":null,\"error\":null}")
    );

    client.wallet(currency(), "listsinceblock", JSONObject.NULL, 1, true);

    JSONObject body = new JSONObject(
      server.takeRequest().getBody().readUtf8()
    );
    assertEquals("listsinceblock", body.getString("method"));
    assertEquals(3, body.getJSONArray("params").length());
    assertTrue(body.getJSONArray("params").isNull(0));
    assertEquals(1, body.getJSONArray("params").getInt(1));
    assertTrue(body.getJSONArray("params").getBoolean(2));
  }

  /**
   * getblockhash and sendtoaddress answer with a bare string, which has no
   * JSONObject to return. Callers should still get something they can read a
   * value out of.
   */
  @Test
  void scalarResultsShouldBeWrapped() {
    server.enqueue(
      new MockResponse().setBody("{\"result\":\"000abc\",\"error\":null}")
    );

    JSONObject result = client.daemon(currency(), "getblockhash", 5);

    assertNotNull(result);
    assertEquals("000abc", result.optString(BitcoinRpcClient.RESULT_KEY, null));
  }

  /**
   * bitcoind reports an RPC failure as HTTP 500 with the reason in the body, so
   * the body has to be read before the status is judged.
   */
  @Test
  void rpcErrorsShouldReturnNullEvenOnHttp500() {
    server.enqueue(
      new MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
        .setBody(
          "{\"result\":null,\"error\":{\"code\":-5," +
          "\"message\":\"Invalid or non-wallet transaction id\"}}"
        )
    );

    assertNull(client.wallet(currency(), "gettransaction", "deadbeef"));
  }

  /**
   * The withdrawal path needs to know which of Core's reasons applied, so the
   * detailed variant has to hand back the code and message the plain one logs
   * and discards.
   */
  @Test
  void detailedCallsShouldKeepTheRpcError() {
    server.enqueue(
      new MockResponse()
        .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
        .setBody(
          "{\"result\":null,\"error\":{\"code\":-6," +
          "\"message\":\"Insufficient funds\"}}"
        )
    );

    RpcResponse response = client.walletDetailed(
      currency(),
      "sendtoaddress",
      "bc1qaaa",
      "0.5"
    );

    assertFalse(response.isSuccess());
    assertTrue(response.refused());
    assertFalse(response.isTransportFailure());
    assertEquals(-6, response.errorCode());
    assertEquals("Insufficient funds", response.errorMessage());
  }

  @Test
  void detailedCallsShouldReportAnUnreadableAnswerAsTransportFailure() {
    server.enqueue(new MockResponse().setBody("not json at all"));

    RpcResponse response = client.walletDetailed(currency(), "getbalances");

    assertTrue(response.isTransportFailure());
    assertFalse(response.refused());
    assertNull(response.errorCode());
  }

  @Test
  void detailedCallsShouldWrapScalarResultsLikeThePlainOnes() {
    server.enqueue(
      new MockResponse().setBody("{\"result\":\"txid123\",\"error\":null}")
    );

    RpcResponse response = client.walletDetailed(
      currency(),
      "sendtoaddress",
      "bc1qaaa",
      "0.5"
    );

    assertTrue(response.isSuccess());
    assertEquals(
      "txid123",
      response.result().optString(BitcoinRpcClient.RESULT_KEY, null)
    );
  }

  @Test
  void nullErrorShouldNotBeTreatedAsAFailure() {
    server.enqueue(
      new MockResponse().setBody("{\"result\":{\"ok\":true},\"error\":null}")
    );

    assertNotNull(client.wallet(currency(), "getbalances"));
  }

  @Test
  void unreadableBodiesShouldReturnNullRatherThanThrow() {
    server.enqueue(new MockResponse().setBody("not json at all"));

    assertNull(client.daemon(currency(), "getblockchaininfo"));
  }

  @Test
  void missingUrlShouldReturnNullRatherThanThrow() {
    CurrencyEntity currency = currency();
    currency.setWalletRpcUrl(null);

    assertNull(client.wallet(currency, "getbalances"));
  }

  @Test
  void basicAuthShouldTolerateAnEmptyPassword() {
    assertEquals(
      "Basic " +
      Base64.getEncoder()
        .encodeToString("anon:".getBytes(StandardCharsets.UTF_8)),
      BitcoinRpcClient.basicAuth("anon", null)
    );
  }
}
