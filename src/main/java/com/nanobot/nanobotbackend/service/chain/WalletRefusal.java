package com.nanobot.nanobotbackend.service.chain;

/**
 * Why a hot wallet would not build a withdrawal, in terms a user can act on.
 *
 * <p>One classification serves both places a refusal can surface: the fee
 * quote at confirmation, where nothing has been debited yet, and the queue
 * processor, where the balance has to be refunded. The {@code reason} is
 * written neutrally so each caller can add its own framing. Raw RPC text is
 * for the log only and never reaches a user.
 *
 * @param kind what went wrong, which decides whether to wait, refund or reject
 * @param reason one user-facing sentence, ending in a full stop
 * @param hint optional follow-up line rendered as small text, or null
 */
public record WalletRefusal(Kind kind, String reason, String hint) {
  public enum Kind {
    /** With the fee taken out of the output, nothing would be left to send. */
    FEE_EXCEEDS_AMOUNT,
    /** Enough money in total, but not enough unlocked yet. Clears with time. */
    FUNDS_LOCKED,
    /** The wallet does not hold enough, unlocked or otherwise. */
    INSUFFICIENT_FUNDS,
    /** Too many inputs for a single transaction. */
    TX_TOO_LARGE,
    /** The destination address was rejected by the wallet or node. */
    ADDRESS_REJECTED,
    /** The wallet or its daemon did not answer at all. */
    UNREACHABLE,
    /** Any other refusal. */
    OTHER,
  }

  public WalletRefusal(Kind kind, String reason) {
    this(kind, reason, null);
  }

  /**
   * The fee taken out of the output would leave nothing to send. The hint
   * names the current floor so the user knows what would succeed.
   */
  public static WalletRefusal feeExceedsAmount(
    String formattedMinimum,
    String ticker
  ) {
    return new WalletRefusal(
      Kind.FEE_EXCEEDS_AMOUNT,
      FEE_EXCEEDS_REASON,
      formattedMinimum == null || formattedMinimum.isBlank()
        ? null
        : "The current minimum withdrawal, including network fees, is **" +
        formattedMinimum +
        " " +
        ticker +
        "**."
    );
  }

  /** Same refusal without naming a floor that has not actually been measured. */
  public static WalletRefusal feeExceedsAmount() {
    return new WalletRefusal(Kind.FEE_EXCEEDS_AMOUNT, FEE_EXCEEDS_REASON);
  }

  private static final String FEE_EXCEEDS_REASON =
    "The network fee would exceed the amount requested, so the " +
    "transaction could not be created.";

  /** Whether waiting can change the outcome. */
  public boolean isTransient() {
    return kind == Kind.FUNDS_LOCKED || kind == Kind.UNREACHABLE;
  }

  /** Wording for a rejection at confirmation time, before any debit. */
  public String confirmationMessage() {
    return "### " + reason + " Nothing has been debited." + hintLine();
  }

  /** Wording for the refund notice after a queued send was refused. */
  public String refundMessage() {
    return (
      "## " +
      reason +
      " Your funds were **not** sent and have been returned to your balance." +
      hintLine()
    );
  }

  private String hintLine() {
    return hint == null || hint.isBlank() ? "" : "\n-# " + hint;
  }
}
