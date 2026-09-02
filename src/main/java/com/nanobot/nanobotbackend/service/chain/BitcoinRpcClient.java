package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * JSON-RPC client for bitcoind.
 *
 * <p>Much smaller than its Monero counterpart because Bitcoin Core accepts HTTP
 * Basic, which the JDK can build in one header, rather than the digest scheme
 * monero-wallet-rpc insists on.
 *
 * <p>Two differences from {@link MoneroWalletRpcClient} are worth knowing. Both
 * the daemon and the wallet require credentials here, where monerod is open and
 * only the wallet authenticates. And Bitcoin takes RPC parameters positionally,
 * as a JSON array, so callers pass an ordered list rather than a named object -
 * which means optional arguments can only be skipped from the right, and any
 * earlier one has to be passed explicitly at its default.
 */
@Component
public class BitcoinRpcClient {

  private final HttpClient client;
  private final FileLogger fileLogger;

  public BitcoinRpcClient() {
    this(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    );
  }

  BitcoinRpcClient(HttpClient client) {
    this.client = client;
    this.fileLogger = new FileLogger("BitcoinRpcClient");
  }

  /**
   * Calls a wallet-scoped method.
   *
   * <p>The wallet is selected by the {@code /wallet/<name>} path already baked
   * into walletRpcUrl. That is not optional decoration: bitcoind rejects an
   * unqualified wallet call with "Wallet file not specified" whenever more than
   * one wallet is loaded, and this node also holds unrelated wallets.
   */
  public JSONObject wallet(
    CurrencyEntity currency,
    String method,
    Object... params
  ) {
    return call(
      currency.getWalletRpcUrl(),
      currency.getWalletRpcUser(),
      currency.getWalletRpcPassword(),
      method,
      params
    );
  }

  /**
   * Calls a node-level method such as getblockchaininfo, on the wallet-less
   * endpoint. Credentials are the wallet's, since bitcoind authenticates every
   * RPC against the same user.
   */
  public JSONObject daemon(
    CurrencyEntity currency,
    String method,
    Object... params
  ) {
    return call(
      currency.getNodeUrl(),
      currency.getWalletRpcUser(),
      currency.getWalletRpcPassword(),
      method,
      params
    );
  }

  /**
   * Returns the JSON-RPC {@code result}, or null on any transport, HTTP or RPC
   * error. Errors are logged and callers treat null as "skip this pass" rather
   * than retrying, since these run on a one second interval.
   *
   * <p>A result that is not an object - getblockhash returns a bare string,
   * getblockcount a number - is wrapped under {@link #RESULT_KEY} so every
   * caller can keep working with a JSONObject.
   */
  public JSONObject call(
    String url,
    String user,
    String password,
    String method,
    Object... params
  ) {
    if (url == null || url.isBlank()) {
      fileLogger.error("No RPC url configured for method " + method);
      return null;
    }

    JSONArray arguments = new JSONArray();
    if (params != null) {
      for (Object param : params) {
        arguments.put(param == null ? JSONObject.NULL : param);
      }
    }

    JSONObject body = new JSONObject()
      .put("jsonrpc", "1.0")
      .put("id", "nanobot")
      .put("method", method)
      .put("params", arguments);

    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()));

      if (user != null && !user.isBlank()) {
        builder.header("Authorization", basicAuth(user, password));
      }

      HttpResponse<String> response = client.send(
        builder.build(),
        HttpResponse.BodyHandlers.ofString()
      );

      // bitcoind reports an RPC-level failure as HTTP 500 with the reason in the
      // body, so the body is parsed before the status is judged. Reading the
      // status alone would reduce every such failure to "500", losing the
      // message that says which one it was.
      JSONObject parsed = parse(response.body());

      if (parsed != null && !parsed.isNull("error")) {
        JSONObject error = parsed.optJSONObject("error");
        fileLogger.error(
          "RPC " +
          method +
          " error " +
          (error == null ? "" : error.optInt("code") + ": ") +
          (error == null
              ? parsed.get("error").toString()
              : error.optString("message"))
        );
        return null;
      }

      if (response.statusCode() != 200) {
        fileLogger.error(
          "RPC " + method + " to " + url + " returned " + response.statusCode()
        );
        return null;
      }

      if (parsed == null) {
        fileLogger.error("RPC " + method + " returned an unreadable body");
        return null;
      }

      return unwrap(parsed);
    } catch (Exception e) {
      fileLogger.error(
        "RPC " + method + " to " + url + " failed: " + e.getMessage()
      );
      return null;
    }
  }

  /** Key holding a scalar or array result that has no object to return. */
  public static final String RESULT_KEY = "result";

  private JSONObject unwrap(JSONObject parsed) {
    Object result = parsed.opt(RESULT_KEY);
    if (result instanceof JSONObject object) {
      return object;
    }
    // createwallet and friends legitimately answer null on success, so an absent
    // result is still a success; only an error means failure.
    return new JSONObject().put(RESULT_KEY, result == null
        ? JSONObject.NULL
        : result);
  }

  private JSONObject parse(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      return new JSONObject(body);
    } catch (Exception e) {
      return null;
    }
  }

  static String basicAuth(String user, String password) {
    String credentials = user + ":" + (password == null ? "" : password);
    return "Basic " +
      Base64.getEncoder()
        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
