package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.BaseDto;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

  @Test
  void defaultConstructorSetsNullId() {
    BaseEntity entity = new BaseEntity();
    assertNull(entity.getId());
  }

  @Test
  void stringConstructorSetsId() {
    BaseEntity entity = new BaseEntity("id-1");
    assertEquals("id-1", entity.getId());
  }

  @Test
  void baseDtoConstructorSetsId() {
    BaseDto dto = new BaseDto("id-2");
    BaseEntity entity = new BaseEntity(dto);
    assertEquals("id-2", entity.getId());
  }

  @Test
  void setIdUpdatesValue() {
    BaseEntity entity = new BaseEntity();
    entity.setId("id-3");
    assertEquals("id-3", entity.getId());
  }

  @Test
  void toJsonExcludesNullsWhenIncludeNullsFalse() {
    BaseEntity entity = new BaseEntity("id-1");
    String json = entity.toJson(false);
    assertNotNull(json);
    assertTrue(json.contains("id-1"));
  }

  @Test
  void toJsonIncludesNullsWhenIncludeNullsTrue() {
    BaseEntity entity = new BaseEntity();
    String json = entity.toJson(true);
    assertNotNull(json);
    assertTrue(json.contains("id"));
  }
}
