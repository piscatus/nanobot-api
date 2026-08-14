package com.nanobot.nanobotbackend.util;

import java.util.function.Consumer;

public class NumberUtil {

  public static boolean validateNumber(
    String num,
    String type,
    int precision,
    Consumer<String> processValidNumber,
    boolean isDollar
  ) {
    if (!isDollar && "all".equalsIgnoreCase(num)) {
      processValidNumber.accept("0");
      return true;
    } else if (num == null || !num.matches("(\\d+(\\.\\d+)?)|(\\.\\d+)")) {
      processValidNumber.accept(
        "Invalid Input: Quantity of " + num + " is invalid!"
      );
      return false;
    } else if ("0".equals(num)) {
      processValidNumber.accept("Invalid Input: Quantity of zero is invalid!");
      return false;
    }

    String[] parts = num.split("\\.");
    int decimalPlaces = (parts.length > 1) ? parts[1].length() : 0;
    if (decimalPlaces > precision) {
      processValidNumber.accept(
        "Invalid Input: The number specified for " +
        type +
        " cannot exceed " +
        precision +
        " decimal places."
      );
      return false;
    }

    processValidNumber.accept(num);
    return true;
  }

  public static boolean validateDollarNumber(
    String num,
    Consumer<String> processValidNumber
  ) {
    if (num == null || num.isEmpty()) {
      return false;
    }

    boolean hasLeadingDollar = num.startsWith("$");
    boolean hasTrailingDollar = num.endsWith("$");

    if (hasLeadingDollar == hasTrailingDollar) {
      return false;
    }

    if (hasLeadingDollar) {
      num = num.substring(1);
    } else {
      num = num.substring(0, num.length() - 1);
    }

    num = num.trim();

    return validateNumber(
      num,
      "dollar amount",
      Constants.dollarToCryptoDecimalPlaces,
      processValidNumber,
      true
    );
  }
}
