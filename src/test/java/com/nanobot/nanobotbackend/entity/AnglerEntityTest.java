package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import java.util.Date;
import org.junit.jupiter.api.Test;

class AnglerEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    AnglerEntity entity = new AnglerEntity();
    assertNull(entity.getGuildId());
    assertNull(entity.getUserId());
    assertFalse(entity.getResting());
  }

  @Test
  void fourArgConstructorSetsFields() {
    Date ts = new Date();
    AnglerEntity entity = new AnglerEntity("g1", "u1", true, ts);

    assertEquals("g1", entity.getGuildId());
    assertEquals("u1", entity.getUserId());
    assertTrue(entity.getResting());
    assertEquals(ts, entity.getTimestamp());
  }

  @Test
  void fromDtoCopiesFields() {
    AnglerDto dto = new AnglerDto();
    dto.setId("a-1");
    dto.setGuildId("g1");
    dto.setUserId("u1");
    dto.setResting(true);

    AnglerEntity entity = new AnglerEntity(dto);

    assertEquals("a-1", entity.getId());
    assertEquals("g1", entity.getGuildId());
    assertEquals("u1", entity.getUserId());
    assertTrue(entity.getResting());
    assertNotNull(entity.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    AnglerEntity entity = new AnglerEntity();
    entity.setGuildId("g2");
    entity.setUserId("u2");
    entity.setResting(false);
    Date ts = new Date();
    entity.setTimestamp(ts);

    assertEquals("g2", entity.getGuildId());
    assertEquals("u2", entity.getUserId());
    assertFalse(entity.getResting());
    assertEquals(ts, entity.getTimestamp());
  }
}
