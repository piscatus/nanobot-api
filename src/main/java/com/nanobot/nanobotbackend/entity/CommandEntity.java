package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "commands")
public class CommandEntity extends BaseEntity {

  @Indexed(unique = true)
  private String name;

  @Indexed(unique = true)
  private String commandId;

  private StatusDto status;

  public CommandEntity() {
    super();
  }

  public CommandEntity(CommandDto dto) {
    super(dto.getId());
    this.name = dto.getName();
    this.commandId = dto.getCommandId();
    this.status = dto.getStatus();
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
