package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.CommandEntity;

public class CommandDto extends BaseDto {

  private String name;

  private String commandId;

  private StatusDto status;

  public CommandDto() {
    super();
  }

  public CommandDto(String name, String commandId) {
    super();
    this.name = name;
    this.commandId = commandId;
  }

  public CommandDto(CommandEntity entity) {
    super(entity.getId());
    this.name = entity.getName();
    this.commandId = entity.getCommandId();
    this.status = entity.getStatus();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCommandId() {
    return commandId;
  }

  public void setCommandId(String commandId) {
    this.commandId = commandId;
  }

  public StatusDto getStatus() {
    return status;
  }

  public void setStatus(StatusDto status) {
    this.status = status;
  }
}
