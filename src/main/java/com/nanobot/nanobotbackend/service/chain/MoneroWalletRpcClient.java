package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * JSON-RPC client for monero-wallet-rpc and monerod.
 *
 * <p>Implements HTTP Digest authentication by hand. monero-wallet-rpc only
 * offers digest, never basic, and the JDK's HttpClient supports basic only, so
 * the alternatives were this or an extra HTTP dependency.
 */
@Component
public class MoneroWalletRpcClient {

  private static final String JSON_RPC_PATH = "/json_rpc";

  private final HttpClient client;
  private final FileLogger fileLogger;
  private final SecureRandom random = new SecureRandom();

  public MoneroWalletRpcClient() {
    this(
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    );
  }

  MoneroWalletRpcClient(HttpClient client) {
    this.client = client;
    this.fileLogger = new FileLogger("MoneroWalletRpcClient");
  }

  /** Calls the wallet RPC configured on the currency. */
  public JSONObject wallet(
    CurrencyEntity currency,
    String method,
    JSONObject params
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
   * Like {@link #wallet}, but keeps the failure detail. For the withdrawal
   * path, which has to tell a wallet that refused from a wallet that is down.
   */
  public RpcResponse walletDetailed(
    CurrencyEntity currency,
    String method,
    JSONObject params
  ) {
    return callDetailed(
      currency.getWalletRpcUrl(),
      currency.getWalletRpcUser(),
      currency.getWalletRpcPassword(),
      method,
      params
    );
  }

  /**
   * Calls the daemon RPC. Used only for cheap height checks, so that the
   * expensive wallet scan runs only when the chain actually moved.
   */
  public JSONObject daemon(
    CurrencyEntity currency,
    String method,
    JSONObject params
  ) {
    return call(currency.getNodeUrl(), null, null, method, params);
  }

  /**
   * Returns the JSON-RPC {@code result} object, or null on any transport error,
   * HTTP error or RPC error. Errors are logged; callers treat null as "skip this
   * pass" rather than retrying blindly, since these run on a short interval.
   */
  public JSONObject call(
    String url,
    String user,
    String password,
    String method,
    JSONObject params
  ) {
    return callDetailed(url, user, password, method, params).result();
  }

  /**
   * The same call with the outcome preserved: a successful result, an RPC
   * error with its code and message, or a transport failure. Everything is
   * still logged here so callers do not have to.
   */
  public RpcResponse callDetailed(
    String url,
    String user,
    String password,
    String method,
    JSONObject params
  ) {
    if (url == null || url.isBlank()) {
      fileLogger.error("No RPC url configured for method " + method);
      return RpcResponse.unreachable("no RPC url configured");
    }
    JSONObject body = new JSONObject()
      .put("jsonrpc", "2.0")
      .put("id", "0")
      .put("method", method);
    if (params != null) {
      body.put("params", params);
    }
    try {
      HttpResponse<String> response = send(url, body, null);

      if (response.statusCode() == 401 && user != null) {
        String challenge = response
          .headers()
          .firstValue("WWW-Authenticate")
          .orElse(null);
        if (challenge == null) {
          fileLogger.error("401 from " + url + " without a digest challenge");
          return RpcResponse.unreachable("401 without a digest challenge");
        }
        String authorization = buildDigestHeader(
          challenge,
          user,
          password,
          "POST",
          JSON_RPC_PATH
        );
        if (authorization == null) {
          return RpcResponse.unreachable("unusable digest challenge");
        }
        response = send(url, body, authorization);
      }

      if (response.statusCode() != 200) {
        fileLogger.error(
          "RPC " + method + " to " + url + " returned " + response.statusCode()
        );
        return RpcResponse.unreachable("HTTP " + response.statusCode());
      }

      JSONObject parsed = new JSONObject(response.body());
      if (parsed.has("error")) {
        JSONObject error = parsed.getJSONObject("error");
        Integer code = error.has("code") ? error.optInt("code") : null;
        String message = error.optString("message");
        fileLogger.error("RPC " + method + " error " + code + ": " + message);
        return RpcResponse.refused(code, message);
      }
      JSONObject result = parsed.optJSONObject("result");
      if (result == null) {
        fileLogger.error("RPC " + method + " returned no result object");
        return RpcResponse.unreachable("no result object");
      }
      return RpcResponse.success(result);
    } catch (Exception e) {
      fileLogger.error(
        "RPC " + method + " to " + url + " failed: " + e.getMessage()
      );
      return RpcResponse.unreachable(String.valueOf(e.getMessage()));
    }
  }

  private HttpResponse<String> send(
    String url,
    JSONObject body,
    String authorization
  ) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .timeout(Duration.ofSeconds(60))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
    if (authorization != null) {
      builder.header("Authorization", authorization);
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  String buildDigestHeader(
    String challenge,
    String user,
    String password,
    String httpMethod,
    String uri
  ) {
    Map<String, String> fields = parseChallenge(challenge);
    String realm = fields.get("realm");
    String nonce = fields.get("nonce");
    if (realm == null || nonce == null) {
      fileLogger.error("Digest challenge missing realm or nonce");
      return null;
    }
    String qop = fields.get("qop");
    String opaque = fields.get("opaque");
    String algorithm = fields.getOrDefault("algorithm", "MD5");
    String cnonce = randomHex();
    String nc = "00000001";

    String ha1 = md5(user + ":" + realm + ":" + password);
    if ("MD5-sess".equalsIgnoreCase(algorithm)) {
      ha1 = md5(ha1 + ":" + nonce + ":" + cnonce);
    }
    String ha2 = md5(httpMethod + ":" + uri);

    String responseHash;
    if (qop != null && !qop.isBlank()) {
      // A challenge can advertise auth and auth-int. Only auth is implemented,
      // and it is what monero-wallet-rpc asks for.
      String selectedQop = "auth";
      responseHash = md5(
        ha1 +
        ":" +
        nonce +
        ":" +
        nc +
        ":" +
        cnonce +
        ":" +
        selectedQop +
        ":" +
        ha2
      );
      StringBuilder header = new StringBuilder("Digest username=\"")
        .append(user)
        .append("\", realm=\"")
        .append(realm)
        .append("\", nonce=\"")
        .append(nonce)
        .append("\", uri=\"")
        .append(uri)
        .append("\", qop=")
        .append(selectedQop)
        .append(", nc=")
        .append(nc)
        .append(", cnonce=\"")
        .append(cnonce)
        .append("\", response=\"")
        .append(responseHash)
        .append("\", algorithm=")
        .append(algorithm);
      if (opaque != null) {
        header.append(", opaque=\"").append(opaque).append("\"");
      }
      return header.toString();
    }

    responseHash = md5(ha1 + ":" + nonce + ":" + ha2);
    return "Digest username=\"" +
      user +
      "\", realm=\"" +
      realm +
      "\", nonce=\"" +
      nonce +
      "\", uri=\"" +
      uri +
      "\", response=\"" +
      responseHash +
      "\"";
  }

  static Map<String, String> parseChallenge(String challenge) {
    Map<String, String> fields = new HashMap<>();
    String stripped = challenge.trim();
    if (stripped.regionMatches(true, 0, "Digest", 0, 6)) {
      stripped = stripped.substring(6).trim();
    }
    Matcher matcher = Pattern
      .compile("(\\w+)=(?:\"([^\"]*)\"|([^,]*))")
      .matcher(stripped);
    while (matcher.find()) {
      String value = matcher.group(2) != null
        ? matcher.group(2)
        : matcher.group(3);
      fields.put(matcher.group(1).toLowerCase(), value.trim());
    }
    return fields;
  }

  private String randomHex() {
    byte[] bytes = new byte[8];
    random.nextBytes(bytes);
    return toHex(bytes);
  }

  static String md5(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      return toHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("MD5 unavailable", e);
    }
  }

  private static String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
