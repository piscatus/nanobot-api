package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class RequestDto extends BaseDto {

  private String address;

  /**
   * Which answer button a user pressed on a trivia drop (0-based). Sent with a
   * pickup request; absent on plain drops. Setter-bound like {@code ticker}.
   */
  private Integer answerIndex;

  /**
   * Trivia category requested by a /triviadrop, or null for any. Setter-bound
   * so existing constructor call sites keep compiling.
   */
  private String category;

  private String channelId;

  private boolean confirmation;

  /** Trivia difficulty requested by a /triviadrop (easy, medium, hard) or null for any. */
  private String difficulty;

  private String dropId;

  private Integer duration;

  /**
   * Leftover seconds on a /triviadrop (0–59). Null means none were asked for.
   * The 10-second floor applies only when duration minutes is not set.
   */
  private Integer seconds;

  private boolean global;

  private String guildId;

  private String roleId;

  private String input;

  private String messageData;

  private Integer random;

  private List<String> receiverIds;

  /**
   * Optional currency ticker narrowing a request to a single currency. Set by
   * the fishing command when the user picks a currency, so the catch is drawn
   * only from that currency's creatures. Absent from the constructor so the
   * existing call sites keep compiling; Jackson binds it through the setter.
   */
  private String ticker;

  private String userId;

  /**
   * Display name of the requesting user. Stored on a drop so the closing embed
   * can name the creator even when Discord has not cached the member and a
   * mention renders as a raw id.
   */
  private String username;

  private List<String> userIdsWithRole;

  private List<String> userRoles;

  private Integer users;

  public RequestDto(
    String address,
    String channelId,
    boolean confirmation,
    String dropId,
    Integer duration,
    boolean global,
    String guildId,
    String roleId,
    String input,
    String messageData,
    Integer random,
    List<String> receiverIds,
    String userId,
    List<String> userIdsWithRole,
    List<String> userRoles,
    Integer users
  ) {
    this.address = address;
    this.channelId = channelId;
    this.confirmation = confirmation;
    this.dropId = dropId;
    this.duration = duration;
    this.global = global;
    this.guildId = guildId;
    this.roleId = roleId;
    this.input = input;
    this.messageData = messageData;
    this.random = random;
    this.receiverIds = receiverIds;
    this.userId = userId;
    this.userIdsWithRole = userIdsWithRole;
    this.userRoles = userRoles;
    this.users = users;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Integer getAnswerIndex() {
    return answerIndex;
  }

  public void setAnswerIndex(Integer answerIndex) {
    this.answerIndex = answerIndex;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public boolean getConfirmation() {
    return confirmation;
  }

  public void setConfirmation(boolean confirmation) {
    this.confirmation = confirmation;
  }

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public String getDropId() {
    return dropId;
  }

  public void setDropId(String dropId) {
    this.dropId = dropId;
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

  public boolean getGlobal() {
    return global;
  }

  public void setGlobal(boolean global) {
    this.global = global;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public String getMessageData() {
    return messageData;
  }

  public void setMessageData(String messageData) {
    this.messageData = messageData;
  }

  public Integer getRandom() {
    return random;
  }

  public void setRandom(Integer random) {
    this.random = random;
  }

  public List<String> getReceiverIds() {
    return receiverIds;
  }

  public void setReceiverIds(List<String> receiverIds) {
    this.receiverIds = receiverIds;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
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

  public List<String> getUserIdsWithRole() {
    return userIdsWithRole;
  }

  public void setUserIdsWithRole(List<String> userIdsWithRole) {
    this.userIdsWithRole = userIdsWithRole;
  }

  public List<String> getUserRoles() {
    return userRoles;
  }

  public void setUserRoles(List<String> userRoles) {
    this.userRoles = userRoles;
  }

  public Integer getUsers() {
    return users;
  }

  public void setUsers(Integer users) {
    this.users = users;
  }
}
