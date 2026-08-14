package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.MessageEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageDtoTest {

  @Test
  void fromEntityCopiesAllFields() {
    MessageEntity entity = new MessageEntity();
    entity.setId("m-1");
    entity.setUserId("u1");
    entity.setGuildId("g1");
    entity.setChannelId("ch1");
    entity.setMessageId("msg1");
    entity.setTitle("Title");
    entity.setColor("#FF0000");
    entity.setContent("content");
    Date ts = new Date();
    entity.setTimestamp(ts);
    entity.setUrl("http://url");
    entity.setList(List.of(Map.of("key", "value")));
    entity.setFooter("footer");

    MessageDto dto = new MessageDto(entity);

    assertEquals("m-1", dto.getId());
    assertEquals("u1", dto.getUserId());
    assertEquals("g1", dto.getGuildId());
    assertEquals("ch1", dto.getChannelId());
    assertEquals("msg1", dto.getMessageId());
    assertEquals("Title", dto.getTitle());
    assertEquals("#FF0000", dto.getColor());
    assertEquals("content", dto.getContent());
    assertEquals(ts, dto.getTimestamp());
    assertEquals("http://url", dto.getUrl());
    assertEquals("footer", dto.getFooter());
  }

  @Test
  void fullConstructorSetsFields() {
    Date ts = new Date();
    List<Map<String, Object>> list = List.of(Map.of("k", "v"));
    MessageDto dto = new MessageDto("u1", "g1", "ch1", "msg1", "Title", "#00FF00", "content", ts, "http://u", list, "footer");

    assertEquals("u1", dto.getUserId());
    assertEquals("g1", dto.getGuildId());
    assertEquals("ch1", dto.getChannelId());
    assertEquals("msg1", dto.getMessageId());
    assertEquals("Title", dto.getTitle());
    assertEquals("#00FF00", dto.getColor());
    assertEquals("content", dto.getContent());
    assertEquals(ts, dto.getTimestamp());
    assertEquals("http://u", dto.getUrl());
    assertEquals(list, dto.getList());
    assertEquals("footer", dto.getFooter());
  }

  @Test
  void settersUpdateFields() {
    MessageDto dto = new MessageDto("u1", "g1", "ch1", "msg1", "T", "c", "cnt", new Date(), null, null, null);
    dto.setUserId("u2");
    dto.setContent("new content");

    assertEquals("u2", dto.getUserId());
    assertEquals("new content", dto.getContent());
  }
}
