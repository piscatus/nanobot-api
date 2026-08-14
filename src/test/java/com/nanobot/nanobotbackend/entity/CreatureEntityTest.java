package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import org.junit.jupiter.api.Test;

class CreatureEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    CreatureEntity entity = new CreatureEntity();
    assertNull(entity.getName());
    assertEquals(0, entity.getCapacity());
    assertEquals(0, entity.getOdds());
  }

  @Test
  void fromDtoCopiesAllFields() {
    CreatureDto dto = new CreatureDto();
    dto.setId("cr-1");
    dto.setName("fish");
    dto.setPluralization("fish");
    dto.setCapacity(100);
    dto.setOdds(50);
    dto.setValue("1");
    dto.setTicker("NANO");
    dto.setEmoji("🐟");
    dto.setImage("fish.png");

    CreatureEntity entity = new CreatureEntity(dto);

    assertEquals("cr-1", entity.getId());
    assertEquals("fish", entity.getName());
    assertEquals("fish", entity.getPluralization());
    assertEquals(100, entity.getCapacity());
    assertEquals(50, entity.getOdds());
    assertEquals("1", entity.getValue());
    assertEquals("NANO", entity.getTicker());
    assertEquals("🐟", entity.getEmoji());
    assertEquals("fish.png", entity.getImage());
  }

  @Test
  void settersUpdateFields() {
    CreatureEntity entity = new CreatureEntity();
    entity.setName("shark");
    entity.setCapacity(200);
    entity.setOdds(10);
    entity.setValue("10");

    assertEquals("shark", entity.getName());
    assertEquals(200, entity.getCapacity());
    assertEquals(10, entity.getOdds());
    assertEquals("10", entity.getValue());
  }
}
