package com.nanobot.nanobotbackend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;
import uk.oczadly.karl.jnano.model.HexData;
import uk.oczadly.karl.jnano.model.NanoAccount;

class CryptoUtilTest {

  private static final String VALID_SEED =
    "0000000000000000000000000000000000000000000000000000000000000001";
  private static final String OTHER_SEED =
    "0000000000000000000000000000000000000000000000000000000000000002";

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

  @Test
  void deriveAddressAtIndexZeroMatchesSeedDefault() {
    String fromSeed = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "XNO");
    String fromIndexZero = CryptoUtil.deriveAddress(
      VALID_SEED,
      0L,
      null,
      "XNO"
    );

    assertEquals(fromSeed, fromIndexZero);
  }

  @Test
  void deriveAddressAtIndexOneDiffersFromIndexZero() {
    String indexZero = CryptoUtil.deriveAddress(VALID_SEED, 0L, null, "XNO");
    String indexOne = CryptoUtil.deriveAddress(VALID_SEED, 1L, null, "XNO");

    assertNotEquals(indexZero, indexOne);
    assertTrue(indexOne.startsWith("nano_"));
  }

  @Test
  void deriveAddressAtMaxIndexDiffersFromIndexZero() {
    String indexZero = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "XNO");
    String maxIndex = CryptoUtil.deriveAddress(
      VALID_SEED,
      CryptoUtil.MAX_ACCOUNT_INDEX,
      null,
      "XNO"
    );

    assertNotEquals(indexZero, maxIndex);
    assertTrue(maxIndex.startsWith("nano_"));
  }

  @Test
  void deriveAddressWithInvalidIndexFallsBackToIndexZero() {
    String indexZero = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "XNO");

    assertEquals(
      indexZero,
      CryptoUtil.deriveAddress(VALID_SEED, null, null, "XNO")
    );
    assertEquals(
      indexZero,
      CryptoUtil.deriveAddress(VALID_SEED, -1L, null, "XNO")
    );
    assertEquals(
      indexZero,
      CryptoUtil.deriveAddress(VALID_SEED, 4294967296L, null, "XNO")
    );
  }

  @Test
  void deriveAddressWithValidPrivateKeyDiffersFromSeed() {
    String seedAddress = CryptoUtil.deriveAddressFromSeed(VALID_SEED, "XNO");
    String privateKeyAddress = CryptoUtil.deriveAddress(
      VALID_SEED,
      0L,
      OTHER_SEED,
      "XNO"
    );

    assertNotEquals(seedAddress, privateKeyAddress);
    assertEquals(
      NanoAccount.fromPrivateKey(new HexData(OTHER_SEED)).toAddress(),
      privateKeyAddress
    );
  }

  @Test
  void deriveAddressWithInvalidPrivateKeyFallsBackToSeedAndIndex() {
    String indexOne = CryptoUtil.deriveAddress(VALID_SEED, 1L, null, "XNO");
    String withInvalidKey = CryptoUtil.deriveAddress(
      VALID_SEED,
      1L,
      "not-a-key",
      "XNO"
    );

    assertEquals(indexOne, withInvalidKey);
  }

  @Test
  void deriveAddressPrefersPrivateKeyOverIndex() {
    String fromPrivateKey = CryptoUtil.deriveAddress(
      VALID_SEED,
      1L,
      OTHER_SEED,
      "XNO"
    );
    String fromIndex = CryptoUtil.deriveAddress(VALID_SEED, 1L, null, "XNO");
    String fromRawKey = NanoAccount.fromPrivateKey(
      new HexData(OTHER_SEED)
    ).toAddress();

    assertEquals(fromRawKey, fromPrivateKey);
    assertNotEquals(fromIndex, fromPrivateKey);
  }

  @Test
  void deriveAddressAppliesBanPrefixForCustomIndex() {
    String address = CryptoUtil.deriveAddress(VALID_SEED, 1L, null, "BAN");

    assertTrue(address.startsWith("ban_"), address);
  }

  @Test
  void isValidIndexAcceptsFullUnsignedRange() {
    assertTrue(CryptoUtil.isValidIndex(0L));
    assertTrue(CryptoUtil.isValidIndex(1L));
    assertTrue(CryptoUtil.isValidIndex(CryptoUtil.MAX_ACCOUNT_INDEX));
    assertFalse(CryptoUtil.isValidIndex(null));
    assertFalse(CryptoUtil.isValidIndex(-1L));
    assertFalse(CryptoUtil.isValidIndex(CryptoUtil.MAX_ACCOUNT_INDEX + 1));
  }

  @Test
  void isValidPrivateKeyRequires64HexCharacters() {
    assertTrue(
      CryptoUtil.isValidPrivateKey(
        "0123456789abcdef0123456789ABCDEF0123456789abcdef0123456789ABCDEF"
      )
    );
    assertFalse(CryptoUtil.isValidPrivateKey(null));
    assertFalse(CryptoUtil.isValidPrivateKey("abc"));
    assertFalse(
      CryptoUtil.isValidPrivateKey(
        "not-hex-not-hex-not-hex-not-hex-not-hex-not-hex-not-hex-not-hex-12"
      )
    );
  }

  @Test
  void canResolvePrivateKeyWhenSeedOrPrivateKeyPresent() {
    assertTrue(CryptoUtil.canResolvePrivateKey(VALID_SEED, null));
    assertTrue(CryptoUtil.canResolvePrivateKey(null, OTHER_SEED));
    assertFalse(CryptoUtil.canResolvePrivateKey(null, "bad"));
    assertFalse(CryptoUtil.canResolvePrivateKey(null, null));
  }

  @Test
  void deriveAddressOverloadsReadUserDetailsAndQueueFields() {
    String expected = CryptoUtil.deriveAddress(VALID_SEED, 1L, null, "XNO");

    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setSeed(VALID_SEED);
    entity.setIndex(1L);
    assertEquals(expected, CryptoUtil.deriveAddress(entity, "XNO"));

    UserDetailsDto dto = new UserDetailsDto();
    dto.setSeed(VALID_SEED);
    dto.setIndex(1L);
    assertEquals(expected, CryptoUtil.deriveAddress(dto, "XNO"));
  }

  @Test
  void applySigningMaterialCopiesIndexAndPrivateKey() {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setIndex(3L);
    user.setPrivateKey(OTHER_SEED);

    QueueDto queue = new QueueDto(
      "u1",
      "src",
      "tgt",
      LevelDto.RECEIVE,
      "h",
      "r",
      "XNO",
      false,
      VALID_SEED,
      new Date(),
      null
    );
    CryptoUtil.applySigningMaterial(queue, user);

    assertEquals(3L, queue.getIndex());
    assertEquals(OTHER_SEED, queue.getPrivateKey());

    QueueEntity queueEntity = new QueueEntity(queue);
    assertEquals(
      CryptoUtil.resolvePrivateKey(null, null, OTHER_SEED).toString(),
      CryptoUtil.resolvePrivateKey(queueEntity).toString()
    );
  }
}
