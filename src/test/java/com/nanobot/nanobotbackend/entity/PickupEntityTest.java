package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.PickupDto;
import java.util.Date;
import org.junit.jupiter.api.Test;

class PickupEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    PickupEntity entity = new PickupEntity();
    assertNull(entity.getDropId());
    assertNull(entity.getUserId());
    assertNull(entity.getTimestamp());
  }

  @Test
  void fromDtoCopiesFields() {
    PickupDto dto = new PickupDto();
    dto.setId("p-1");
    dto.setDropId("drop-1");
    dto.setUserId("u1");

    PickupEntity entity = new PickupEntity(dto);

    assertEquals("p-1", entity.getId());
    assertEquals("drop-1", entity.getDropId());
    assertEquals("u1", entity.getUserId());
    assertNotNull(entity.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    PickupEntity entity = new PickupEntity();
    entity.setDropId("drop-2");
    entity.setUserId("u2");
    Date ts = new Date();
    entity.setTimestamp(ts);

    assertEquals("drop-2", entity.getDropId());
    assertEquals("u2", entity.getUserId());
    assertEquals(ts, entity.getTimestamp());
  }
}
