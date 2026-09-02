package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import org.junit.jupiter.api.Test;

class ChainSettingsTest {

  private static CurrencyEntity withConfirmations(String confirmations) {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setConfirmations(confirmations);
    return currency;
  }

  @Test
  void shouldParseAStoredNumber() {
    assertEquals(42L, ChainSettings.parseLong("42", 7L));
    assertEquals(42L, ChainSettings.parseLong("  42  ", 7L));
  }

  /** Every one of these reaches the parser from a hand-edited document. */
  @Test
  void shouldFallBackOnAnythingUnreadable() {
    assertEquals(7L, ChainSettings.parseLong(null, 7L));
    assertEquals(7L, ChainSettings.parseLong("", 7L));
    assertEquals(7L, ChainSettings.parseLong("   ", 7L));
    assertEquals(7L, ChainSettings.parseLong("six", 7L));
    assertEquals(7L, ChainSettings.parseLong("1.5", 7L));
  }

  @Test
  void shouldReadAConfiguredConfirmationDepth() {
    assertEquals(10, ChainSettings.confirmations(withConfirmations("10"), 6));
  }

  /**
   * Crediting at zero confirmations would let an unconfirmed transaction be
   * spent, so a zero or negative setting is treated as unset rather than
   * honoured.
   */
  @Test
  void shouldRefuseAZeroOrNegativeConfirmationDepth() {
    assertEquals(6, ChainSettings.confirmations(withConfirmations("0"), 6));
    assertEquals(6, ChainSettings.confirmations(withConfirmations("-3"), 6));
  }

  @Test
  void shouldFallBackWhenConfirmationsAreUnset() {
    assertEquals(6, ChainSettings.confirmations(withConfirmations(null), 6));
    assertEquals(6, ChainSettings.confirmations(withConfirmations("many"), 6));
  }
}
