package com.nanobot.nanobotbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import org.springframework.data.annotation.Id;

public class BaseResponseDto {

  private String errorMessage;

  private List<CommandDto> commands;

  private GuildConfigurationsDto guildConfigurations;

  private UserDetailsDto userDetails;

  public BaseResponseDto() {
    this.errorMessage = null;
  }

  public BaseResponseDto(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public void setCommands(List<CommandDto> commands) {
    this.commands = commands;
  }

  public List<CommandDto> getCommands() {
    return commands;
  }

  public GuildConfigurationsDto getGuildConfigurations() {
    return guildConfigurations;
  }

  public void setGuildConfigurations(
    GuildConfigurationsDto guildConfigurations
  ) {
    this.guildConfigurations = guildConfigurations;
  }

  public UserDetailsDto getUserDetails() {
    return userDetails;
  }

  public void setUserDetails(UserDetailsDto userDetails) {
    this.userDetails = userDetails;
  }

  public String toJson(boolean includeNulls) {
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();

    if (!includeNulls) {
      mapperBuilder.changeDefaultPropertyInclusion(
        incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
      );
    }

    try {
      return mapperBuilder.build().writeValueAsString(this);
    } catch (JacksonException e) {
      e.printStackTrace();
      return null;
    }
  }
}
