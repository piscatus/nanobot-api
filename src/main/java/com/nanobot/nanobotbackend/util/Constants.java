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
  /** Scale for converting USD amounts into crypto units (matches max fractional digits on $ input). */
  public static final int dollarToCryptoDecimalPlaces = 8;
}
