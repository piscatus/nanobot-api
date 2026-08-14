package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.MessageDto;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    MessageEntity entity = new MessageEntity();
    assertNull(entity.getUserId());
    assertNull(entity.getGuildId());
    assertNull(entity.getChannelId());
    assertNull(entity.getContent());
  }

  @Test
  void fromDtoCopiesAllFields() {
    Date ts = new Date();
    List<Map<String, Object>> list = List.of(Map.of("key", "value"));
    MessageDto dto = new MessageDto("u1", "g1", "ch1", "msg1", "Title", "#FF0000", "content", ts, "http://url", list, "footer");
    dto.setId("m-1");

    MessageEntity entity = new MessageEntity(dto);

    assertEquals("m-1", entity.getId());
    assertEquals("u1", entity.getUserId());
    assertEquals("g1", entity.getGuildId());
    assertEquals("ch1", entity.getChannelId());
    assertEquals("msg1", entity.getMessageId());
    assertEquals("Title", entity.getTitle());
    assertEquals("#FF0000", entity.getColor());
    assertEquals("content", entity.getContent());
    assertEquals(ts, entity.getTimestamp());
    assertEquals("http://url", entity.getUrl());
    assertEquals(list, entity.getList());
    assertEquals("footer", entity.getFooter());
  }

  @Test
  void settersUpdateFields() {
    MessageEntity entity = new MessageEntity();
    entity.setUserId("u2");
    entity.setGuildId("g2");
    entity.setChannelId("ch2");
    entity.setContent("new content");

    assertEquals("u2", entity.getUserId());
    assertEquals("g2", entity.getGuildId());
    assertEquals("ch2", entity.getChannelId());
    assertEquals("new content", entity.getContent());
  }
}
