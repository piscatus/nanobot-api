package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.AnglerEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;

class AnglerDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    AnglerDto dto = new AnglerDto();
    assertNull(dto.getGuildId());
    assertNull(dto.getUserId());
    assertNull(dto.getTimestamp());
  }

  @Test
  void fromEntityCopiesAllFields() {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("a-1");
    entity.setGuildId("g1");
    entity.setUserId("u1");
    entity.setResting(true);
    Date ts = new Date();
    entity.setTimestamp(ts);

    AnglerDto dto = new AnglerDto(entity);

    assertEquals("a-1", dto.getId());
    assertEquals("g1", dto.getGuildId());
    assertEquals("u1", dto.getUserId());
    assertTrue(dto.getResting());
    assertEquals(ts, dto.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    AnglerDto dto = new AnglerDto();
    dto.setGuildId("g2");
    dto.setUserId("u2");
    dto.setResting(false);
    Date ts = new Date();
    dto.setTimestamp(ts);

    assertEquals("g2", dto.getGuildId());
    assertEquals("u2", dto.getUserId());
    assertFalse(dto.getResting());
    assertEquals(ts, dto.getTimestamp());
  }
}
