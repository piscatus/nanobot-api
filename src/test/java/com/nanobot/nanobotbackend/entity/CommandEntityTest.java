package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import org.junit.jupiter.api.Test;

class CommandEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    CommandEntity entity = new CommandEntity();
    assertNull(entity.getName());
    assertNull(entity.getCommandId());
    assertNull(entity.getStatus());
  }

  @Test
  void fromDtoCopiesAllFields() {
    CommandDto dto = new CommandDto("balance", "cmd-123");
    dto.setId("cmd-1");
    dto.setStatus(StatusDto.ACTIVE);

    CommandEntity entity = new CommandEntity(dto);

    assertEquals("cmd-1", entity.getId());
    assertEquals("balance", entity.getName());
    assertEquals("cmd-123", entity.getCommandId());
    assertEquals(StatusDto.ACTIVE, entity.getStatus());
  }

  @Test
  void settersUpdateFields() {
    CommandEntity entity = new CommandEntity();
    entity.setName("transfer");
    entity.setCommandId("cmd-456");
    entity.setStatus(StatusDto.LOCKED);

    assertEquals("transfer", entity.getName());
    assertEquals("cmd-456", entity.getCommandId());
    assertEquals(StatusDto.LOCKED, entity.getStatus());
  }
}
