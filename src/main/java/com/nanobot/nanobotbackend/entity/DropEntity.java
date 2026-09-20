package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TriviaQuestionDto;
import java.util.Date;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "drops")
public class DropEntity extends BaseEntity {

  @Indexed
  private String channelId;

  private Integer duration;

  /** Leftover seconds on a trivia drop; null on plain drops and on whole minutes. */
  private Integer seconds;

  private Date endTime;

  @Indexed
  private String guildId;

  private String maximumEntries;

  private String messageData;

  private String messageId;

  private Integer numberWinners;

  private String input;

  private String userId;

  /** Creator's display name, so the closing embed can name them even when a
   * mention renders as a raw id for uncached members. */
  private String username;

  private Date startTime;

  private TransferDto transfer;

  private String requiredRole;

  /**
   * Present only on trivia drops. Its answers are shown as buttons and only
   * pickups that chose the correct index can win.
   */
  private TriviaQuestionDto trivia;

  public DropEntity() {
    super();
  }

  public DropEntity(DropDto drop) {
    super(drop.getId());
    this.channelId = drop.getChannelId();
    this.duration = drop.getDuration();
    this.seconds = drop.getSeconds();
    this.endTime = drop.getEndTime();
    this.guildId = drop.getGuildId();
    this.input = drop.getInput();
    this.maximumEntries = drop.getMaximumEntries();
    this.messageData = drop.getMessageData();
    this.messageId = drop.getMessageId();
    this.numberWinners = drop.getNumberWinners();
    this.requiredRole = drop.getRequiredRole();
    this.startTime = drop.getStartTime();
    this.transfer = drop.getTransfer();
    this.userId = drop.getUserId();
    this.username = drop.getUsername();
    this.trivia = drop.getTrivia();
  }

  /**
   * True for a trivia drop, which pays only correct answers. Named has* rather
   * than is* so Jackson does not take it for a second "trivia" property.
   */
  public boolean hasTrivia() {
    return trivia != null;
  }

  public TriviaQuestionDto getTrivia() {
    return trivia;
  }

  public void setTrivia(TriviaQuestionDto trivia) {
    this.trivia = trivia;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public Integer getDuration() {
    return duration;
  }

  public void setDuration(Integer duration) {
    this.duration = duration;
  }

  public Integer getSeconds() {
    return seconds;
  }

  public void setSeconds(Integer seconds) {
    this.seconds = seconds;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public String getMaximumEntries() {
    return maximumEntries;
  }

  public void setMaximumEntries(String maximumEntries) {
    this.maximumEntries = maximumEntries;
  }

  public String getMessageData() {
    return messageData;
  }

  public void setMessageData(String messageData) {
    this.messageData = messageData;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public Integer getNumberWinners() {
    return numberWinners;
  }

  public void setNumberWinners(Integer numberWinners) {
    this.numberWinners = numberWinners;
  }

  public String getRequiredRole() {
    return requiredRole;
  }

  public void setRequiredRole(String requiredRole) {
    this.requiredRole = requiredRole;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public TransferDto getTransfer() {
    return transfer;
  }

  public void setTransfer(TransferDto transfer) {
    this.transfer = transfer;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
