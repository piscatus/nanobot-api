package com.nanobot.nanobotbackend.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CryptoUtilTest {

  private static final String VALID_SEED =
    "0000000000000000000000000000000000000000000000000000000000000001";

  @Test
  void getRandomKeyShouldReturnNonEmptyHexString() {
    String key = CryptoUtil.getRandomKey();

    assertNotNull(key);
    assertFalse(key.isEmpty());
    assertTrue(key.matches("[0-9a-fA-F]+"), "Key should be hex: " + key);
  }

  @Test
  void deriveAddressFromSeedShouldReturnNanoAddressForNonBanTicker() {
    String address = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "XNO");

    assertNotNull(address);
    assertTrue(
      address.startsWith("nano_"),
      "XNO address should start with nano_: " + address
    );
  }

  @Test
  void deriveAddressFromSeedShouldReturnBananoAddressForBanTicker() {
    String address = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "BAN");

    assertNotNull(address);
    assertTrue(
      address.startsWith("ban_"),
      "BAN address should start with ban_: " + address
    );
  }

  @Test
  void deriveAddressFromSeedShouldReturnBananoAddressForBanTickerCaseInsensitive() {
    String address = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "ban");

    assertNotNull(address);
    assertTrue(
      address.startsWith("ban_"),
      "ban ticker should produce ban_ address: " + address
    );
  }

  @Test
  void getAccountFromSeedShouldReturnNonNullAccount() {
    var account = CryptoUtil.getAccountFromSeed(VALID_SEED);

    assertNotNull(account);
    assertNotNull(account.toAddress());
  }

  @Test
  void getPrivateKeyFromSeedShouldReturnNonNullHexData() {
    var key = CryptoUtil.getPrivateKeyFromSeed(VALID_SEED);

    assertNotNull(key);
    assertNotNull(key.toString());
  }
}
