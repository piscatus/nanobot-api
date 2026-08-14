package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.CreatureEntity;
import org.junit.jupiter.api.Test;

class CreatureDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    CreatureDto dto = new CreatureDto();
    assertNull(dto.getName());
    assertEquals(0, dto.getCapacity());
  }

  @Test
  void fromEntityCopiesAllFields() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("cr-1");
    entity.setName("Fish");
    entity.setPluralization("Fish");
    entity.setCapacity(10);
    entity.setOdds(5);
    entity.setValue("1");
    entity.setTicker("NANO");
    entity.setEmoji("🐟");
    entity.setImage("fish.png");

    CreatureDto dto = new CreatureDto(entity);

    assertEquals("cr-1", dto.getId());
    assertEquals("Fish", dto.getName());
    assertEquals("Fish", dto.getPluralization());
    assertEquals(10, dto.getCapacity());
    assertEquals(5, dto.getOdds());
    assertEquals("1", dto.getValue());
    assertEquals("NANO", dto.getTicker());
    assertEquals("🐟", dto.getEmoji());
    assertEquals("fish.png", dto.getImage());
  }

  @Test
  void threeArgConstructorSetsCoreFields() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");

    assertEquals("Fish", dto.getName());
    assertEquals("Fish", dto.getPluralization());
    assertEquals("🐟", dto.getEmoji());
  }

  @Test
  void fourArgConstructorSetsCoreFields() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟", "NANO");

    assertEquals("Fish", dto.getName());
    assertEquals("Fish", dto.getPluralization());
    assertEquals("🐟", dto.getEmoji());
    assertEquals("NANO", dto.getTicker());
  }

  @Test
  void settersUpdateFields() {
    CreatureDto dto = new CreatureDto();
    dto.setName("Shark");
    dto.setPluralization("Sharks");
    dto.setCapacity(20);
    dto.setOdds(2);
    dto.setValue("10");
    dto.setTicker("XNO");
    dto.setEmoji("🦈");
    dto.setImage("shark.png");

    assertEquals("Shark", dto.getName());
    assertEquals("Sharks", dto.getPluralization());
    assertEquals(20, dto.getCapacity());
    assertEquals(2, dto.getOdds());
    assertEquals("10", dto.getValue());
    assertEquals("XNO", dto.getTicker());
    assertEquals("🦈", dto.getEmoji());
    assertEquals("shark.png", dto.getImage());
  }
}
