package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nanobot.nanobotbackend.entity.ActivityEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class ActivityDtoTest {

  @Test
  void testNoArgConstructorAndSetters() {
    ActivityDto dto = new ActivityDto();

    Date now = new Date();
    dto.setId("activity123");
    dto.setGuildId("guild001");
    dto.setChannelId("channel001");
    dto.setUserId("user001");
    dto.setTimestamp(now);

    assertEquals("activity123", dto.getId());
    assertEquals("guild001", dto.getGuildId());
    assertEquals("channel001", dto.getChannelId());
    assertEquals("user001", dto.getUserId());
    assertEquals(now, dto.getTimestamp());
  }

  @Test
  void testAllArgsConstructor() {
    Date timestamp = new Date();
    ActivityDto dto = new ActivityDto("guildABC", "channelXYZ", "user999", timestamp);

    assertEquals("guildABC", dto.getGuildId());
    assertEquals("channelXYZ", dto.getChannelId());
    assertEquals("user999", dto.getUserId());
    assertEquals(timestamp, dto.getTimestamp());
  }

  @Test
  void testConstructorFromActivityEntity() {
    Date timestamp = new Date();

    ActivityEntity entity = new ActivityEntity();
    entity.setGuildId("guild456");
    entity.setChannelId("channel789");
    entity.setUserId("user321");
    entity.setTimestamp(timestamp);

    ActivityDto dto = new ActivityDto(entity);

    assertEquals("guild456", dto.getGuildId());
    assertEquals("channel789", dto.getChannelId());
    assertEquals("user321", dto.getUserId());
    assertEquals(timestamp, dto.getTimestamp());
  }
}