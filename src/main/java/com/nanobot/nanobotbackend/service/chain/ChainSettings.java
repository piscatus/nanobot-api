package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;

/**
 * Numeric settings read off a currency document.
 *
 * <p>These fields are stored as strings and edited by hand, so every read has
 * to survive a null, a blank, or a typo. Each adapter had its own copy of that
 * defensiveness; getting it wrong in one of them would mean a confirmation
 * depth or a scan cursor silently becoming zero.
 */
final class ChainSettings {

  private ChainSettings() {}

  /** Parses a stored number, falling back when it is absent or unreadable. */
  static long parseLong(String value, long fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  /**
   * Confirmation depth for a currency.
   *
   * <p>A configured zero or negative is treated as unset rather than honoured:
   * crediting at zero confirmations would let an unconfirmed transaction be
   * spent, which is the one outcome the setting exists to prevent.
   */
  static int confirmations(CurrencyEntity currencyEntity, int fallback) {
    int configured = (int) parseLong(
      currencyEntity.getConfirmations(),
      fallback
    );
    return configured > 0 ? configured : fallback;
  }
}
