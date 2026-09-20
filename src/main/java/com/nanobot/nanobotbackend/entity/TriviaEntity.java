package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.TriviaDto;
import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One question in the trivia bank that /triviadrop draws from.
 *
 * <p>Answers are shown on Discord buttons, whose labels are capped at 80
 * characters, so the service refuses any answer longer than that on insert.
 * {@code enabled} gates use: generated questions are imported disabled and
 * switched on after a human has reviewed them.
 */
@Document(collection = "trivias")
public class TriviaEntity extends BaseEntity {

  @Indexed
  private String category;

  /** "multiple" (four answers) or "boolean" (True/False). */
  private String type;

  /** "easy", "medium" or "hard". */
  private String difficulty;

  private String question;

  private String correctAnswer;

  private List<String> incorrectAnswers;

  /** Where the question came from: "opentdb", "generated" or "manual". */
  private String source;

  /** SHA-1 of the normalized question text, unique so imports cannot duplicate. */
  @Indexed(unique = true)
  private String hash;

  private boolean enabled;

  /** Last time a drop used this question; null when it never has. */
  private Date lastUsed;

  private Integer timesUsed;

  public TriviaEntity() {
    super();
  }

  public TriviaEntity(TriviaDto trivia) {
    super(trivia.getId());
    this.category = trivia.getCategory();
    this.type = trivia.getType();
    this.difficulty = trivia.getDifficulty();
    this.question = trivia.getQuestion();
    this.correctAnswer = trivia.getCorrectAnswer();
    this.incorrectAnswers = trivia.getIncorrectAnswers();
    this.source = trivia.getSource();
    this.hash = trivia.getHash();
    this.enabled = trivia.getEnabled() != null && trivia.getEnabled();
    this.lastUsed = trivia.getLastUsed();
    this.timesUsed = trivia.getTimesUsed() == null ? 0 : trivia.getTimesUsed();
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getCorrectAnswer() {
    return correctAnswer;
  }

  public void setCorrectAnswer(String correctAnswer) {
    this.correctAnswer = correctAnswer;
  }

  public List<String> getIncorrectAnswers() {
    return incorrectAnswers;
  }

  public void setIncorrectAnswers(List<String> incorrectAnswers) {
    this.incorrectAnswers = incorrectAnswers;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Date getLastUsed() {
    return lastUsed;
  }

  public void setLastUsed(Date lastUsed) {
    this.lastUsed = lastUsed;
  }

  public Integer getTimesUsed() {
    return timesUsed;
  }

  public void setTimesUsed(Integer timesUsed) {
    this.timesUsed = timesUsed;
  }
}
