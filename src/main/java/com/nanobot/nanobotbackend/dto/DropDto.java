package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.DropEntity;
import java.util.Date;

public class DropDto extends BaseDto {

  private String channelId;

  private Integer duration;

  /** Leftover seconds on a trivia drop; null on plain drops and on whole minutes. */
  private Integer seconds;

  private Date endTime;

  private String guildId;

  private String input;

  private String maximumEntries;

  private String messageData;

  private String messageId;

  private Integer numberWinners;

  private String requiredRole;

  private Date startTime;

  private TransferDto transfer;

  private String userId;

  private String username;

  /** Present only on trivia drops; see {@link TriviaQuestionDto}. */
  private TriviaQuestionDto trivia;

  public DropDto() {
    super();
  }

  public DropDto(DropEntity entity) {
    super(entity.getId());
    this.channelId = entity.getChannelId();
    this.duration = entity.getDuration();
    this.seconds = entity.getSeconds();
    this.endTime = entity.getEndTime();
    this.guildId = entity.getGuildId();
    this.input = entity.getInput();
    this.maximumEntries = entity.getMaximumEntries();
    this.messageData = entity.getMessageData();
    this.messageId = entity.getMessageId();
    this.numberWinners = entity.getNumberWinners();
    this.requiredRole = entity.getRequiredRole();
    this.startTime = entity.getStartTime();
    this.transfer = entity.getTransfer();
    this.userId = entity.getUserId();
    this.username = entity.getUsername();
    this.trivia = entity.getTrivia();
  }

  public DropDto(
    String channelId,
    Integer duration,
    Date endTime,
    String guildId,
    String input,
    String maximumEntries,
    String messageData,
    Integer numberWinners,
    String requiredRole,
    Date startTime,
    TransferDto transfer,
    String userId
  ) {
    super();
    this.channelId = channelId;
    this.duration = duration;
    this.endTime = endTime;
    this.guildId = guildId;
    this.input = input;
    this.maximumEntries = maximumEntries;
    this.messageData = messageData;
    this.numberWinners = numberWinners;
    this.requiredRole = requiredRole;
    this.startTime = startTime;
    this.transfer = transfer;
    this.userId = userId;
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

  public TriviaQuestionDto getTrivia() {
    return trivia;
  }

  public void setTrivia(TriviaQuestionDto trivia) {
    this.trivia = trivia;
  }
}
