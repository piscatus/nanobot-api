package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.TriviaEntity;
import java.util.Date;
import java.util.List;

public class TriviaDto extends BaseDto {

  private String category;

  private String type;

  private String difficulty;

  private String question;

  private String correctAnswer;

  private List<String> incorrectAnswers;

  private String source;

  private String hash;

  private Boolean enabled;

  private Date lastUsed;

  private Integer timesUsed;

  public TriviaDto() {
    super();
  }

  public TriviaDto(TriviaEntity entity) {
    super(entity.getId());
    this.category = entity.getCategory();
    this.type = entity.getType();
    this.difficulty = entity.getDifficulty();
    this.question = entity.getQuestion();
    this.correctAnswer = entity.getCorrectAnswer();
    this.incorrectAnswers = entity.getIncorrectAnswers();
    this.source = entity.getSource();
    this.hash = entity.getHash();
    this.enabled = entity.getEnabled();
    this.lastUsed = entity.getLastUsed();
    this.timesUsed = entity.getTimesUsed();
  }

  public TriviaDto(
    String category,
    String type,
    String difficulty,
    String question,
    String correctAnswer,
    List<String> incorrectAnswers,
    String source,
    Boolean enabled
  ) {
    super();
    this.category = category;
    this.type = type;
    this.difficulty = difficulty;
    this.question = question;
    this.correctAnswer = correctAnswer;
    this.incorrectAnswers = incorrectAnswers;
    this.source = source;
    this.enabled = enabled;
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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
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
