package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.TriviaEntity;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TriviaQuestionDtoTest {

  @Test
  void fromEntityKeepsAllAnswersAndPointsCorrectIndexAtTheCorrectAnswer() {
    TriviaEntity entity = multipleChoice("Paris", List.of("London", "Berlin", "Madrid"));

    TriviaQuestionDto dto = TriviaQuestionDto.fromEntity(entity, new Random(42));

    assertEquals(4, dto.getAnswers().size());
    assertTrue(dto.getAnswers().containsAll(List.of("Paris", "London", "Berlin", "Madrid")));
    assertEquals("Paris", dto.getAnswers().get(dto.getCorrectIndex()));
    assertTrue(dto.isCorrect(dto.getCorrectIndex()));
  }

  @Test
  void fromEntityPutsTrueBeforeFalseForBooleanQuestions() {
    TriviaEntity entity = new TriviaEntity();
    entity.setId("bool-1");
    entity.setType("boolean");
    entity.setQuestion("Is water wet?");
    entity.setCorrectAnswer("False");
    entity.setIncorrectAnswers(List.of("True"));

    TriviaQuestionDto dto = TriviaQuestionDto.fromEntity(entity, new Random(42));

    assertEquals(List.of("True", "False"), dto.getAnswers());
    assertEquals(1, dto.getCorrectIndex());
    assertEquals("False", dto.getCorrectAnswer());
  }

  @Test
  void isCorrectIsNullSafe() {
    TriviaQuestionDto dto = new TriviaQuestionDto();
    dto.setAnswers(List.of("A", "B", "C", "D"));
    dto.setCorrectIndex(2);

    assertFalse(dto.isCorrect(null));
    assertFalse(dto.isCorrect(0));
    assertTrue(dto.isCorrect(2));
  }

  @Test
  void getCorrectAnswerReturnsNullWhenIndexIsInvalid() {
    TriviaQuestionDto dto = new TriviaQuestionDto();
    dto.setAnswers(List.of("A", "B"));

    dto.setCorrectIndex(null);
    assertNull(dto.getCorrectAnswer());

    dto.setCorrectIndex(-1);
    assertNull(dto.getCorrectAnswer());

    dto.setCorrectIndex(2);
    assertNull(dto.getCorrectAnswer());

    dto.setCorrectIndex(1);
    assertEquals("B", dto.getCorrectAnswer());
  }

  private static TriviaEntity multipleChoice(
    String correct,
    List<String> incorrect
  ) {
    TriviaEntity entity = new TriviaEntity();
    entity.setId("q-1");
    entity.setCategory("Science");
    entity.setType("multiple");
    entity.setDifficulty("medium");
    entity.setQuestion("What is the capital of France?");
    entity.setCorrectAnswer(correct);
    entity.setIncorrectAnswers(incorrect);
    return entity;
  }
}
