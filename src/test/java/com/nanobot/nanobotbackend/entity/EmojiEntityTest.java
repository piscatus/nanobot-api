package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.EmojiDto;
import org.junit.jupiter.api.Test;

class EmojiEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    EmojiEntity entity = new EmojiEntity();
    assertNull(entity.getCategory());
    assertNull(entity.getName());
    assertNull(entity.getEmoji());
  }

  @Test
  void threeArgConstructorSetsFields() {
    EmojiEntity entity = new EmojiEntity("cat", "smile", "😺");
    assertEquals("cat", entity.getCategory());
    assertEquals("smile", entity.getName());
    assertEquals("😺", entity.getEmoji());
  }

  @Test
  void fromDtoCopiesAllFields() {
    EmojiDto dto = new EmojiDto();
    dto.setId("e-1");
    dto.setCategory("reactions");
    dto.setName("thumbs_up");
    dto.setEmoji("👍");

    EmojiEntity entity = new EmojiEntity(dto);

    assertEquals("e-1", entity.getId());
    assertEquals("reactions", entity.getCategory());
    assertEquals("thumbs_up", entity.getName());
    assertEquals("👍", entity.getEmoji());
  }

  @Test
  void settersUpdateFields() {
    EmojiEntity entity = new EmojiEntity();
    entity.setCategory("c1");
    entity.setName("n1");
    entity.setEmoji("e1");

    assertEquals("c1", entity.getCategory());
    assertEquals("n1", entity.getName());
    assertEquals("e1", entity.getEmoji());
  }
}
