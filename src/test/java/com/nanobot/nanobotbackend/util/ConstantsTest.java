package com.nanobot.nanobotbackend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Verifies key constants used in transfer logic and other critical paths.
 * Prevents accidental changes that could break TRANSFERS.md invariants.
 */
class ConstantsTest {

  @Test
  void maximumTransferPartsShouldBeFive() {
    assertEquals(5, Constants.maximumTransferParts);
  }

  @Test
  void maximumTransferAttemptsShouldBeThree() {
    assertEquals(3, Constants.maximumTransferAttepts);
  }

  @Test
  void commandNamesShouldBeNonEmpty() {
    assertNotNull(Constants.COMMAND_NAME_FISH);
    assertNotNull(Constants.COMMAND_NAME_GIFT);
    assertNotNull(Constants.COMMAND_NAME_WALLET);
    assertNotNull(Constants.COMMAND_NAME_INVENTORY);
    assertNotNull(Constants.COMMAND_NAME_DROP);
  }

  @Test
  void unknownErrorShouldContainRetryGuidance() {
    assertNotNull(Constants.unknownError);
    assertEquals(
      "An unknown error occured, please try again or contact support. Sorry for the inconvenience!",
      Constants.unknownError
    );
  }
}
