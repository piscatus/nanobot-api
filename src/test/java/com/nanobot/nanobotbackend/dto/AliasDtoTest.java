package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.AliasEntity;
import org.junit.jupiter.api.Test;

class AliasDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    AliasDto dto = new AliasDto();
    assertNull(dto.getGuildId());
    assertNull(dto.getSingular());
    assertNull(dto.getTicker());
  }

  @Test
  void fromEntityCopiesAllFields() {
    AliasEntity entity = new AliasEntity();
    entity.setId("a-1");
    entity.setGuildId("g1");
    entity.setOwnerId("o1");
    entity.setSingular("banana");
    entity.setPlural("bananas");
    entity.setTicker("BAN");
    entity.setValue("1");
    entity.setInput("$1 nano");
    entity.setEmoji("🍌");

    AliasDto dto = new AliasDto(entity);

    assertEquals("a-1", dto.getId());
    assertEquals("g1", dto.getGuildId());
    assertEquals("o1", dto.getOwnerId());
    assertEquals("banana", dto.getSingular());
    assertEquals("bananas", dto.getPlural());
    assertEquals("BAN", dto.getTicker());
    assertEquals("1", dto.getValue());
    assertEquals("$1 nano", dto.getInput());
    assertEquals("🍌", dto.getEmoji());
  }

  @Test
  void sevenArgConstructorSetsFields() {
    AliasDto dto = new AliasDto("g1", "o1", "nano", "nanos", "NANO", "1", "🌿");

    assertEquals("g1", dto.getGuildId());
    assertEquals("o1", dto.getOwnerId());
    assertEquals("nano", dto.getSingular());
    assertEquals("nanos", dto.getPlural());
    assertEquals("NANO", dto.getTicker());
    assertEquals("1", dto.getValue());
    assertEquals("🌿", dto.getEmoji());
  }

  @Test
  void sixArgConstructorSetsFieldsWithoutOwner() {
    AliasDto dto = new AliasDto("g1", "nano", "nanos", "NANO", "1", "🌿");

    assertEquals("g1", dto.getGuildId());
    assertNull(dto.getOwnerId());
    assertEquals("nano", dto.getSingular());
    assertEquals("nanos", dto.getPlural());
    assertEquals("NANO", dto.getTicker());
    assertEquals("1", dto.getValue());
    assertEquals("🌿", dto.getEmoji());
  }
}
