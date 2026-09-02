package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.*;

import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NanoAddressIndexTest {

  private static final String SEED_ONE =
    "1111111111111111111111111111111111111111111111111111111111111111";
  private static final String SEED_TWO =
    "2222222222222222222222222222222222222222222222222222222222222222";

  private NanoAddressIndex index;

  @BeforeEach
  void setUp() {
    index = new NanoAddressIndex();
  }

  private static UserDetailsEntity user(String userId, String seed) {
    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setUserId(userId);
    entity.setSeed(seed);
    return entity;
  }

  @Test
  void rebuildShouldMapEveryDerivedAddressToItsOwner() {
    UserDetailsEntity first = user("user-1", SEED_ONE);
    UserDetailsEntity second = user("user-2", SEED_TWO);

    Map<String, UserDetailsEntity> addresses = index.rebuild(
      "XNO",
      List.of(first, second)
    );

    assertEquals(2, addresses.size());
    assertEquals(
      "user-1",
      index.ownerOf("XNO", CryptoUtil.deriveAddress(first, "XNO"))
    );
    assertEquals(
      "user-2",
      index.ownerOf("XNO", CryptoUtil.deriveAddress(second, "XNO"))
    );
  }

  /**
   * The map the sweep batches over has to be the same derivation the index
   * answers lookups with, or the two halves would disagree about who owns an
   * address.
   */
  @Test
  void rebuildShouldReturnTheSameAddressesItIndexed() {
    UserDetailsEntity first = user("user-1", SEED_ONE);

    Map<String, UserDetailsEntity> addresses = index.rebuild(
      "XNO",
      List.of(first)
    );

    assertEquals(index.addresses("XNO"), List.copyOf(addresses.keySet()));
  }

  @Test
  void rebuildShouldSkipUsersWithNoSigningMaterial() {
    UserDetailsEntity seedless = user("user-3", null);

    Map<String, UserDetailsEntity> addresses = index.rebuild(
      "XNO",
      List.of(seedless)
    );

    assertTrue(addresses.isEmpty());
    assertFalse(index.isPopulated("XNO"));
  }

  @Test
  void tickersShouldNotShareAddresses() {
    UserDetailsEntity first = user("user-1", SEED_ONE);
    index.rebuild("XNO", List.of(first));

    String nanoAddress = CryptoUtil.deriveAddress(first, "XNO");

    assertEquals("user-1", index.ownerOf("XNO", nanoAddress));
    assertNull(index.ownerOf("BAN", nanoAddress));
  }

  @Test
  void addShouldReportOnlyTheFirstRegistrationAsNew() {
    assertTrue(index.add("XNO", "nano_1abc", "user-1"));
    assertFalse(index.add("XNO", "nano_1abc", "user-1"));
    assertEquals("user-1", index.ownerOf("XNO", "nano_1abc"));
  }

  @Test
  void addShouldSurviveARebuildThatIncludesTheSameUser() {
    UserDetailsEntity first = user("user-1", SEED_ONE);
    String address = CryptoUtil.deriveAddress(first, "XNO");

    index.add("XNO", address, "user-1");
    index.rebuild("XNO", List.of(first));

    assertEquals("user-1", index.ownerOf("XNO", address));
  }

  @Test
  void lookupsShouldBeNullSafe() {
    assertNull(index.ownerOf(null, "nano_1abc"));
    assertNull(index.ownerOf("XNO", null));
    assertFalse(index.isKnown("XNO", "nano_1unknown"));
    assertTrue(index.addresses("XNO").isEmpty());
    assertFalse(index.add("XNO", null, "user-1"));
  }
}
