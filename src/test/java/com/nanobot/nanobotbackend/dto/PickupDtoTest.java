package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.PickupEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;

class PickupDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    PickupDto dto = new PickupDto();
    assertNull(dto.getDropId());
    assertNull(dto.getUserId());
    assertNull(dto.getTimestamp());
  }

  @Test
  void fourArgConstructorSetsAllFields() {
    Date ts = new Date();
    PickupDto dto = new PickupDto("p-1", "drop-1", "u1", ts);

    assertEquals("p-1", dto.getId());
    assertEquals("drop-1", dto.getDropId());
    assertEquals("u1", dto.getUserId());
    assertEquals(ts, dto.getTimestamp());
  }

  @Test
  void fromEntityCopiesAllFields() {
    PickupEntity entity = new PickupEntity();
    entity.setId("p-1");
    entity.setDropId("drop-1");
    entity.setUserId("u1");
    Date ts = new Date();
    entity.setTimestamp(ts);

    PickupDto dto = new PickupDto(entity);

    assertEquals("p-1", dto.getId());
    assertEquals("drop-1", dto.getDropId());
    assertEquals("u1", dto.getUserId());
    assertEquals(ts, dto.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    PickupDto dto = new PickupDto();
    dto.setDropId("drop-2");
    dto.setUserId("u2");
    Date ts = new Date();
    dto.setTimestamp(ts);

    assertEquals("drop-2", dto.getDropId());
    assertEquals("u2", dto.getUserId());
    assertEquals(ts, dto.getTimestamp());
  }
}
