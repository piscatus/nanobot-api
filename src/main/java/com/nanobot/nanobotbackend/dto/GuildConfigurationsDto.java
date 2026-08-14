package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.List;

public class GuildConfigurationsDto extends BaseDto {

  private String guildId;
  private String userId;
  private StatusDto status;
  private String fishingLoggingChannelId;
  private String transferLoggingChannelId;
  private String fishingChannelId;
  private String fishingRole;
  private List<String> fishingBypassRoles;
  private String fishingError;
  private Integer fishingFrequency;
  private Integer maximumMinutesActive;
  private Integer maximumActiveUsers;
  private AliasDto aliasData;

  public GuildConfigurationsDto() {
    super();
  }

  public GuildConfigurationsDto(GuildConfigurationsEntity entity) {
    super(entity.getId());
    this.guildId = entity.getGuildId();
    this.status = entity.getStatus();
    this.fishingLoggingChannelId = entity.getFishingLoggingChannelId();
    this.transferLoggingChannelId = entity.getTransferLoggingChannelId();
    this.fishingChannelId = entity.getFishingChannelId();
    this.fishingRole = entity.getFishingRole();
    this.fishingBypassRoles = entity.getFishingBypassRoles();
    this.fishingError = entity.getFishingError();
    this.fishingFrequency = entity.getFishingFrequency();
    this.maximumMinutesActive = entity.getMaximumMinutesActive();
    this.maximumActiveUsers = entity.getMaximumActiveUsers();
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public StatusDto getStatus() {
    return status;
  }

  public void setStatus(StatusDto status) {
    this.status = status;
  }

  public String getFishingLoggingChannelId() {
    return fishingLoggingChannelId;
  }

  public void setFishingLoggingChannelId(String fishingLoggingChannelId) {
    this.fishingLoggingChannelId = fishingLoggingChannelId;
  }

  public String getTransferLoggingChannelId() {
    return transferLoggingChannelId;
  }

  public void setTransferLoggingChannelId(String transferLoggingChannelId) {
    this.transferLoggingChannelId = transferLoggingChannelId;
  }

  public String getFishingChannelId() {
    return fishingChannelId;
  }

  public void setFishingChannelId(String fishingChannelId) {
    this.fishingChannelId = fishingChannelId;
  }

  public String getFishingRole() {
    return fishingRole;
  }

  public void setFishingRole(String fishingRole) {
    this.fishingRole = fishingRole;
  }

  public List<String> getFishingBypassRoles() {
    return fishingBypassRoles;
  }

  public void setFishingBypassRoles(List<String> fishingBypassRoles) {
    this.fishingBypassRoles = fishingBypassRoles;
  }

  public String getFishingError() {
    return fishingError;
  }

  public void setFishingError(String fishingError) {
    this.fishingError = fishingError;
  }

  public Integer getFishingFrequency() {
    return fishingFrequency;
  }

  public void setFishingFrequency(Integer fishingFrequency) {
    this.fishingFrequency = fishingFrequency;
  }

  public Integer getMaximumMinutesActive() {
    return maximumMinutesActive;
  }

  public void setMaximumMinutesActive(Integer maximumMinutesActive) {
    this.maximumMinutesActive = maximumMinutesActive;
  }

  public Integer getMaximumActiveUsers() {
    return maximumActiveUsers;
  }

  public void setMaximumActiveUsers(Integer maximumActiveUsers) {
    this.maximumActiveUsers = maximumActiveUsers;
  }

  public AliasDto getAliasData() {
    return aliasData;
  }

  public void setAliasData(AliasDto aliasData) {
    this.aliasData = aliasData;
  }
}
