package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.AliasDto;
import org.junit.jupiter.api.Test;

class AliasEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    AliasEntity entity = new AliasEntity();
    assertNull(entity.getGuildId());
    assertNull(entity.getOwnerId());
    assertNull(entity.getSingular());
    assertNull(entity.getPlural());
  }

  @Test
  void fromDtoCopiesAllFields() {
    AliasDto dto = new AliasDto("g1", "o1", "banana", "bananas", "BAN", "1", "🍌");
    dto.setId("a-1");
    dto.setInput("$1 ban");

    AliasEntity entity = new AliasEntity(dto);

    assertEquals("a-1", entity.getId());
    assertEquals("g1", entity.getGuildId());
    assertEquals("o1", entity.getOwnerId());
    assertEquals("banana", entity.getSingular());
    assertEquals("bananas", entity.getPlural());
    assertEquals("BAN", entity.getTicker());
    assertEquals("1", entity.getValue());
    assertEquals("$1 ban", entity.getInput());
    assertEquals("🍌", entity.getEmoji());
  }

  @Test
  void settersUpdateFields() {
    AliasEntity entity = new AliasEntity();
    entity.setGuildId("g2");
    entity.setSingular("nano");
    entity.setPlural("nanos");
    entity.setTicker("NANO");

    assertEquals("g2", entity.getGuildId());
    assertEquals("nano", entity.getSingular());
    assertEquals("nanos", entity.getPlural());
    assertEquals("NANO", entity.getTicker());
  }
}
