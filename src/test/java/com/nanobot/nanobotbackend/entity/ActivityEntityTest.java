package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import java.util.Date;
import org.junit.jupiter.api.Test;

class ActivityEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    ActivityEntity entity = new ActivityEntity();
    assertNull(entity.getGuildId());
    assertNull(entity.getChannelId());
    assertNull(entity.getUserId());
    assertNull(entity.getTimestamp());
  }

  @Test
  void fromActivityDtoCopiesAllFields() {
    ActivityDto dto = new ActivityDto("g1", "ch1", "u1", new Date(1000L));
    dto.setId("act-1");

    ActivityEntity entity = new ActivityEntity(dto);

    assertEquals("act-1", entity.getId());
    assertEquals("g1", entity.getGuildId());
    assertEquals("ch1", entity.getChannelId());
    assertEquals("u1", entity.getUserId());
    assertNotNull(entity.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    ActivityEntity entity = new ActivityEntity();
    Date ts = new Date(2000L);
    entity.setGuildId("g2");
    entity.setChannelId("ch2");
    entity.setUserId("u2");
    entity.setTimestamp(ts);

    assertEquals("g2", entity.getGuildId());
    assertEquals("ch2", entity.getChannelId());
    assertEquals("u2", entity.getUserId());
    assertEquals(ts, entity.getTimestamp());
  }
}
