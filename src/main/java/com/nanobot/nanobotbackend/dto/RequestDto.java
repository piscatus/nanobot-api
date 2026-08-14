package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class RequestDto extends BaseDto {

  private String address;

  private String channelId;

  private boolean confirmation;

  private String dropId;

  private Integer duration;

  private boolean global;

  private String guildId;

  private String roleId;

  private String input;

  private String messageData;

  private Integer random;

  private List<String> receiverIds;

  private String userId;

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

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
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
