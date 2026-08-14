package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class FishingReminderResponseDto extends BaseResponseDto {

  private List<AnglerDto> anglers;

  private List<GuildConfigurationsDto> allGuildConfigurations;

  public FishingReminderResponseDto() {
    super();
  }

  public List<AnglerDto> getAnglers() {
    return anglers;
  }

  public void setAnglers(List<AnglerDto> anglers) {
    this.anglers = anglers;
  }

  public List<GuildConfigurationsDto> getAllGuildConfigurations() {
    return allGuildConfigurations;
  }

  public void setAllGuildConfigurations(
    List<GuildConfigurationsDto> allGuildConfigurations
  ) {
    this.allGuildConfigurations = allGuildConfigurations;
  }
}
