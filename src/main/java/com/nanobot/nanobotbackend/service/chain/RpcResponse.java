package com.nanobot.nanobotbackend.service.chain;

import org.json.JSONObject;

/**
 * Outcome of one JSON-RPC call with the failure detail kept.
 *
 * <p>The plain client methods collapse every failure into null, which suits
 * the polling paths: they skip the pass and try again next second. The
 * withdrawal path needs one distinction those methods erase - a wallet that
 * answered and refused, versus a wallet that never answered. The first is a
 * verdict on the request and should be acted on; the second is an outage and
 * should be waited out.
 *
 * @param result the RPC result, or null when the call did not succeed
 * @param refused true when the server answered with an RPC-level error
 * @param errorCode the RPC error code when refused, when the server gave one
 * @param errorMessage the RPC error message when refused, or the transport
 *     failure description otherwise; for logs, never for users
 */
public record RpcResponse(
  JSONObject result,
  boolean refused,
  Integer errorCode,
  String errorMessage
) {
  public static RpcResponse success(JSONObject result) {
    return new RpcResponse(result, false, null, null);
  }

  public static RpcResponse refused(Integer code, String message) {
    return new RpcResponse(null, true, code, message);
  }

  /** No RPC-level answer: connection failure, HTTP error or unreadable body. */
  public static RpcResponse unreachable(String message) {
    return new RpcResponse(null, false, null, message);
  }

  public boolean isSuccess() {
    return result != null;
  }

  public boolean isTransportFailure() {
    return result == null && !refused;
  }

  /**
   * A non-blank string field from a successful result, or null. Callers use
   * this for identifiers such as tx_hash, where an empty string is as useless
   * as a missing field.
   */
  public String resultString(String key) {
    if (result == null || key == null || !result.has(key)) {
      return null;
    }
    String value = result.optString(key, "");
    return value.isBlank() ? null : value;
  }
}
