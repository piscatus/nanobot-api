package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "guildConfigurations")
public class GuildConfigurationsEntity extends BaseEntity {

  @Indexed(unique = true)
  private String guildId;

  private StatusDto status;

  private String fishingLoggingChannelId;

  private String transferLoggingChannelId;

  private String fishingChannelId;

  private String fishingRole;

  private List<String> fishingBypassRoles;

  private String fishingError;

  private Integer fishingFrequency;

  private Integer maximumActiveUsers;

  private Integer maximumMinutesActive;

  private List<WalletDto> wallets;

  public GuildConfigurationsEntity() {
    super();
  }

  public GuildConfigurationsEntity(
    GuildConfigurationsDto guildConfigurationsDto
  ) {
    super(guildConfigurationsDto.getId());
    this.guildId = guildConfigurationsDto.getGuildId();
    this.status = guildConfigurationsDto.getStatus();
    this.fishingLoggingChannelId =
      guildConfigurationsDto.getFishingLoggingChannelId();
    this.transferLoggingChannelId =
      guildConfigurationsDto.getTransferLoggingChannelId();
    this.fishingChannelId = guildConfigurationsDto.getFishingChannelId();
    this.fishingRole = guildConfigurationsDto.getFishingRole();
    this.fishingBypassRoles = guildConfigurationsDto.getFishingBypassRoles();
    this.fishingError = guildConfigurationsDto.getFishingError();
    this.fishingFrequency = guildConfigurationsDto.getFishingFrequency();
    this.maximumActiveUsers = guildConfigurationsDto.getMaximumActiveUsers();
    this.maximumMinutesActive =
      guildConfigurationsDto.getMaximumMinutesActive();
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
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

  public List<WalletDto> getWallets() {
    return wallets;
  }

  public void setWallets(List<WalletDto> wallets) {
    this.wallets = wallets;
  }
}
