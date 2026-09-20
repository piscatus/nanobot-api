package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransferServicesImplNoTriviaMessageTest {

  @Test
  void noTriviaQuestionsMessageWhenNeitherFilterIsSet() {
    assertEquals(
      "There are no trivia questions available right now, sorry!",
      TransferServicesImpl.noTriviaQuestionsMessage(null, null)
    );
  }

  @Test
  void noTriviaQuestionsMessageWhenOnlyCategoryIsSet() {
    assertEquals(
      "There are no trivia questions in the category `X`, sorry! Try another category.",
      TransferServicesImpl.noTriviaQuestionsMessage("X", null)
    );
  }

  @Test
  void noTriviaQuestionsMessageWhenOnlyDifficultyIsSet() {
    assertEquals(
      "There are no hard trivia questions, sorry! Try another difficulty.",
      TransferServicesImpl.noTriviaQuestionsMessage(null, "hard")
    );
  }

  @Test
  void noTriviaQuestionsMessageWhenBothFiltersAreSet() {
    assertEquals(
      "There are no hard trivia questions in the category `X`, sorry! Try another category or difficulty.",
      TransferServicesImpl.noTriviaQuestionsMessage("X", "hard")
    );
  }

  @Test
  void triviaSecondsErrorWhenSecondsAloneAreBelowTheFloor() {
    assertEquals(
      "Trivia drops must last at least `10` seconds when duration_minutes is not set.",
      TransferServicesImpl.triviaSecondsError(0, 5)
    );
  }

  @Test
  void triviaSecondsErrorAllowsSecondsBelowTheFloorWhenMinutesAreSet() {
    assertEquals(null, TransferServicesImpl.triviaSecondsError(2, 5));
  }

  @Test
  void triviaSecondsErrorRejectsSecondsAbove59() {
    assertEquals(
      "Trivia duration seconds must be between `0` and `59`.",
      TransferServicesImpl.triviaSecondsError(0, 60)
    );
  }
}
