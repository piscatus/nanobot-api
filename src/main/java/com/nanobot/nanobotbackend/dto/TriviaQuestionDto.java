package com.nanobot.nanobotbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nanobot.nanobotbackend.entity.TriviaEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The question attached to a trivia drop, embedded on the drop document.
 *
 * <p>Answers are stored already shuffled so button positions differ between
 * drops, and only {@code correctIndex} says which one is right. That field is
 * write-only for JSON so it is persisted and can be posted, but never leaves
 * the API in a response: the bot must not be able to leak it before the drop
 * ends.
 */
public class TriviaQuestionDto {

  private String triviaId;

  private String category;

  private String difficulty;

  private String type;

  private String question;

  private List<String> answers;

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private Integer correctIndex;

  public TriviaQuestionDto() {}

  /**
   * Builds the drop-side view of a bank question, shuffling the answers.
   * True/False questions keep their conventional order; a shuffled position
   * buys nothing there and would only look odd.
   */
  public static TriviaQuestionDto fromEntity(
    TriviaEntity entity,
    Random random
  ) {
    TriviaQuestionDto dto = new TriviaQuestionDto();
    dto.triviaId = entity.getId();
    dto.category = entity.getCategory();
    dto.difficulty = entity.getDifficulty();
    dto.type = entity.getType();
    dto.question = entity.getQuestion();

    List<String> answers = new ArrayList<>();
    answers.add(entity.getCorrectAnswer());
    if (entity.getIncorrectAnswers() != null) {
      answers.addAll(entity.getIncorrectAnswers());
    }

    if ("boolean".equalsIgnoreCase(entity.getType())) {
      answers.sort((a, b) -> Boolean.compare(!isTrue(a), !isTrue(b)));
    } else {
      Collections.shuffle(answers, random);
    }

    dto.answers = answers;
    dto.correctIndex = answers.indexOf(entity.getCorrectAnswer());
    return dto;
  }

  private static boolean isTrue(String answer) {
    return answer != null && answer.trim().equalsIgnoreCase("true");
  }

  /**
   * The correct answer's text, or null when the drop has no valid index.
   * Ignored by Jackson for the same reason correctIndex is write-only: a
   * getter would otherwise serialize it straight into the API response.
   */
  @JsonIgnore
  public String getCorrectAnswer() {
    if (
      answers == null ||
      correctIndex == null ||
      correctIndex < 0 ||
      correctIndex >= answers.size()
    ) {
      return null;
    }
    return answers.get(correctIndex);
  }

  public boolean isCorrect(Integer answerIndex) {
    return answerIndex != null && answerIndex.equals(correctIndex);
  }

  public String getTriviaId() {
    return triviaId;
  }

  public void setTriviaId(String triviaId) {
    this.triviaId = triviaId;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public List<String> getAnswers() {
    return answers;
  }

  public void setAnswers(List<String> answers) {
    this.answers = answers;
  }

  public Integer getCorrectIndex() {
    return correctIndex;
  }

  public void setCorrectIndex(Integer correctIndex) {
    this.correctIndex = correctIndex;
  }
}
