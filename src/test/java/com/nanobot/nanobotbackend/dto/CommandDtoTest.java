package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.CommandEntity;
import org.junit.jupiter.api.Test;

class CommandDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    CommandDto dto = new CommandDto();
    assertNull(dto.getName());
    assertNull(dto.getCommandId());
    assertNull(dto.getStatus());
  }

  @Test
  void nameCommandIdConstructorSetsFields() {
    CommandDto dto = new CommandDto("balance", "cmd-123");

    assertEquals("balance", dto.getName());
    assertEquals("cmd-123", dto.getCommandId());
    assertNull(dto.getStatus());
  }

  @Test
  void fromEntityCopiesAllFields() {
    CommandEntity entity = new CommandEntity();
    entity.setId("cmd-1");
    entity.setName("transfer");
    entity.setCommandId("cmd-456");
    entity.setStatus(StatusDto.ACTIVE);

    CommandDto dto = new CommandDto(entity);

    assertEquals("cmd-1", dto.getId());
    assertEquals("transfer", dto.getName());
    assertEquals("cmd-456", dto.getCommandId());
    assertEquals(StatusDto.ACTIVE, dto.getStatus());
  }

  @Test
  void settersUpdateFields() {
    CommandDto dto = new CommandDto();
    dto.setName("help");
    dto.setCommandId("cmd-789");
    dto.setStatus(StatusDto.LOCKED);

    assertEquals("help", dto.getName());
    assertEquals("cmd-789", dto.getCommandId());
    assertEquals(StatusDto.LOCKED, dto.getStatus());
  }
}
