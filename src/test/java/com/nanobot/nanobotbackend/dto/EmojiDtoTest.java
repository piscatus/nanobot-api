package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.EmojiEntity;
import org.junit.jupiter.api.Test;

class EmojiDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    EmojiDto dto = new EmojiDto();
    assertNull(dto.getCategory());
    assertNull(dto.getName());
    assertNull(dto.getEmoji());
  }

  @Test
  void fromEntityCopiesAllFields() {
    EmojiEntity entity = new EmojiEntity("cat", "smile", "😺");
    entity.setId("e-1");

    EmojiDto dto = new EmojiDto(entity);

    assertEquals("e-1", dto.getId());
    assertEquals("cat", dto.getCategory());
    assertEquals("smile", dto.getName());
    assertEquals("😺", dto.getEmoji());
  }

  @Test
  void settersUpdateFields() {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("reactions");
    dto.setName("thumbs_up");
    dto.setEmoji("👍");

    assertEquals("reactions", dto.getCategory());
    assertEquals("thumbs_up", dto.getName());
    assertEquals("👍", dto.getEmoji());
  }
}
