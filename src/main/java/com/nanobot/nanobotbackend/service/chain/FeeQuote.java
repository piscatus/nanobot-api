package com.nanobot.nanobotbackend.service.chain;

import java.math.BigInteger;

/**
 * The fee the hot wallet would charge for one specific withdrawal right now.
 *
 * <p>Comes from asking the wallet to build the transaction without relaying
 * it, so it reflects the inputs the wallet would actually select rather than
 * an assumed transaction size. {@code UNAVAILABLE} means the wallet could not
 * be asked or is only temporarily unable to answer; callers fall back to the
 * currency's stored estimate. {@code DELAYED} means the withdrawal can be
 * built once locked funds unlock, so the user should choose whether to wait.
 * {@code REJECTED} means the wallet said the withdrawal cannot be built as
 * requested, which is worth telling the user before anything is debited.
 */
public record FeeQuote(Status status, BigInteger fee, WalletRefusal refusal) {
  public enum Status {
    QUOTED,
    DELAYED,
    REJECTED,
    UNAVAILABLE,
  }

  public static FeeQuote quoted(BigInteger fee) {
    return new FeeQuote(Status.QUOTED, fee, null);
  }

  public static FeeQuote delayed(WalletRefusal refusal) {
    return new FeeQuote(Status.DELAYED, null, refusal);
  }

  public static FeeQuote rejected(WalletRefusal refusal) {
    return new FeeQuote(Status.REJECTED, null, refusal);
  }

  public static FeeQuote unavailable() {
    return new FeeQuote(Status.UNAVAILABLE, null, null);
  }

  /** A quoted fee of zero is not a real quote; treat it as unavailable. */
  public static FeeQuote ofFee(BigInteger fee) {
    if (fee == null || fee.signum() <= 0) {
      return unavailable();
    }
    return quoted(fee);
  }
}
