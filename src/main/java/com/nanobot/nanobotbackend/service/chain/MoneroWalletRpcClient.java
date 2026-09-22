package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * JSON-RPC client for monero-wallet-rpc and monerod.
 *
 * <p>Implements HTTP Digest authentication by hand. monero-wallet-rpc only
 * offers digest, never basic, and the JDK's HttpClient supports basic only.
 * The wallet also binds the digest nonce to the TCP connection, so the
 * challenge and the authenticated retry have to share one socket. Two
 * separate client calls open two connections and the retry is refused.
 */
@Component
public class MoneroWalletRpcClient {

  private static final String JSON_RPC_PATH = "/json_rpc";
  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 60_000;

  private final FileLogger fileLogger;
  private final SecureRandom random = new SecureRandom();

  public MoneroWalletRpcClient() {
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
    byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
    try (Socket socket = open(url)) {
      RawHttp response = exchange(socket, URI.create(url), payload, null);

      if (response.status == 401 && user != null) {
        String challenge = selectChallenge(
          response.headers.get("www-authenticate")
        );
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
        // Same socket. The wallet's nonce is only valid on the connection
        // that issued it.
        response = exchange(socket, URI.create(url), payload, authorization);
      }

      if (response.status != 200) {
        fileLogger.error(
          "RPC " + method + " to " + url + " returned " + response.status
        );
        return RpcResponse.unreachable("HTTP " + response.status);
      }

      JSONObject parsed = new JSONObject(response.body);
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

  /**
   * monero-wallet-rpc advertises MD5 and MD5-sess. MD5 is the one curl and
   * the wallet agree on; MD5-sess is rejected in practice.
   */
  static String selectChallenge(List<String> challenges) {
    if (challenges == null || challenges.isEmpty()) {
      return null;
    }
    String fallback = null;
    for (String challenge : challenges) {
      if (challenge == null || challenge.isBlank()) {
        continue;
      }
      String algorithm = parseChallenge(challenge).getOrDefault(
        "algorithm",
        "MD5"
      );
      if ("MD5".equalsIgnoreCase(algorithm)) {
        return challenge;
      }
      if (fallback == null) {
        fallback = challenge;
      }
    }
    return fallback;
  }

  private Socket open(String url) throws IOException {
    URI uri = URI.create(url);
    int port = uri.getPort();
    if (port < 0) {
      port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(uri.getHost(), port), CONNECT_TIMEOUT_MS);
    socket.setSoTimeout(READ_TIMEOUT_MS);
    return socket;
  }

  private RawHttp exchange(
    Socket socket,
    URI uri,
    byte[] payload,
    String authorization
  ) throws IOException {
    String path = uri.getRawPath();
    if (path == null || path.isEmpty()) {
      path = "/";
    }
    if (uri.getRawQuery() != null) {
      path = path + "?" + uri.getRawQuery();
    }
    int port = uri.getPort();
    String host = uri.getHost() + (port > 0 ? ":" + port : "");
    StringBuilder headers = new StringBuilder();
    headers.append("POST ").append(path).append(" HTTP/1.1\r\n");
    headers.append("Host: ").append(host).append("\r\n");
    headers.append("Content-Type: application/json\r\n");
    headers.append("Connection: keep-alive\r\n");
    headers.append("Content-Length: ").append(payload.length).append("\r\n");
    if (authorization != null) {
      headers.append("Authorization: ").append(authorization).append("\r\n");
    }
    headers.append("\r\n");
    OutputStream out = socket.getOutputStream();
    out.write(headers.toString().getBytes(StandardCharsets.US_ASCII));
    out.write(payload);
    out.flush();
    return readHttp(socket.getInputStream());
  }

  private RawHttp readHttp(InputStream in) throws IOException {
    ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
    int matched = 0;
    while (matched < 4) {
      int next = in.read();
      if (next < 0) {
        throw new IOException("connection closed before headers");
      }
      headerBytes.write(next);
      if (next == "\r\n\r\n".charAt(matched)) {
        matched++;
      } else {
        matched = next == '\r' ? 1 : 0;
      }
      if (headerBytes.size() > 65_536) {
        throw new IOException("response headers too large");
      }
    }
    String headerText = headerBytes.toString(StandardCharsets.ISO_8859_1);
    String[] lines = headerText.split("\r\n");
    String[] statusParts = lines[0].split(" ");
    if (statusParts.length < 2) {
      throw new IOException("unreadable status line");
    }
    int status = Integer.parseInt(statusParts[1]);
    Map<String, List<String>> headers = new HashMap<>();
    for (int i = 1; i < lines.length; i++) {
      int colon = lines[i].indexOf(':');
      if (colon < 0) {
        continue;
      }
      String name = lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT);
      String value = lines[i].substring(colon + 1).trim();
      headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
    }
    int length = 0;
    List<String> contentLength = headers.get("content-length");
    if (contentLength != null && !contentLength.isEmpty()) {
      length = Integer.parseInt(contentLength.get(0).trim());
    }
    byte[] body = in.readNBytes(length);
    if (body.length != length) {
      throw new IOException("response body ended early");
    }
    return new RawHttp(status, headers, new String(body, StandardCharsets.UTF_8));
  }

  private record RawHttp(
    int status,
    Map<String, List<String>> headers,
    String body
  ) {}

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
