package com.nanobot.nanobotbackend.util;

public class Constants {

  public static final String COMMAND_NAME_ACTIVE = "active";
  public static final String COMMAND_NAME_ALIASES = "aliases";
  public static final String COMMAND_NAME_AUDIT = "audit";
  public static final String COMMAND_NAME_AWARD = "award";
  public static final String COMMAND_NAME_BONUSES = "bonuses";
  public static final String COMMAND_NAME_CONFIG = "config";
  public static final String COMMAND_NAME_CREATURES = "creatures";
  public static final String COMMAND_NAME_CURRENCIES = "currencies";
  public static final String COMMAND_NAME_CUTE = "cute";
  public static final String COMMAND_NAME_DROP = "drop";
  public static final String COMMAND_NAME_FISH = "fish";
  public static final String COMMAND_NAME_GIFT = "gift";
  public static final String COMMAND_NAME_HELP = "help";
  public static final String COMMAND_NAME_INVENTORY = "inventory";
  public static final String COMMAND_NAME_LEADERBOARDS = "leaderboards";
  public static final String COMMAND_NAME_MERGE = "merge";
  public static final String COMMAND_NAME_PICKUP = "pickup";
  public static final String COMMAND_NAME_RAIN = "rain";
  public static final String COMMAND_NAME_RECEIVE = "receive";
  public static final String COMMAND_NAME_RESERVES = "reserves";
  public static final String COMMAND_NAME_ROLES = "roles";
  public static final String COMMAND_NAME_RULES = "rules";
  public static final String COMMAND_NAME_SELL = "sell";
  public static final String COMMAND_NAME_SEND = "send";
  public static final String COMMAND_NAME_SERVER = "server";
  public static final String COMMAND_NAME_TRANSACTIONS = "transactions";
  public static final String COMMAND_NAME_TRIVIADROP = "triviadrop";
  public static final String COMMAND_NAME_UPDATE = "update";
  public static final String COMMAND_NAME_WALLET = "wallet";
  //
  public static final String unknownError =
    "An unknown error occured, please try again or contact support. Sorry for the inconvenience!";
  //
  public static final int defaultActiveUsers = 40;
  public static final int defaultDropDuration = 30;
  public static final int defaultMinutesActive = 30;
  public static final int maximumTransferParts = 5;
  public static final int maximumTransferAttepts = 3;
  public static final int maximumDropUsers = 10000;
  //
  /** Trivia drops are short by design: a long window is a Google window. */
  public static final int defaultTriviaDropDuration = 3;
  /** A question used within this many days is passed over while others remain. */
  public static final int triviaRecentlyUsedDays = 7;
  /** Discord caps a button label at 80 characters; answers live on buttons. */
  public static final int maximumTriviaAnswerLength = 80;
  public static final int maximumTriviaQuestionLength = 1000;
  /** One action row holds at most five buttons; four answers is the format. */
  public static final int maximumTriviaAnswers = 4;
  /** The difficulty values a question may carry and a /triviadrop may ask for. */
  public static final java.util.List<String> TRIVIA_DIFFICULTIES =
    java.util.List.of("easy", "medium", "hard");
  /** Seconds leftover on a trivia drop. The 10-second floor applies only when minutes is 0. */
  public static final int minimumTriviaSeconds = 10;
  public static final int maximumTriviaSeconds = 59;
  /** Scale for converting USD amounts into crypto units (matches max fractional digits on $ input). */
  public static final int dollarToCryptoDecimalPlaces = 8;
  //
  /**
   * Chain protocol identifiers selecting a ChainAdapter. Currency documents
   * created before this field existed resolve to NANO.
   */
  public static final String PROTOCOL_NANO = "NANO";
  public static final String PROTOCOL_MONERO = "MONERO";
  public static final String PROTOCOL_BITCOIN = "BITCOIN";
}
