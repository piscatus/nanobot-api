package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserDetailsEntityTest {

  @Test
  public void testConstructorFromDto() {
    UserDetailsDto dto = new UserDetailsDto();
    dto.setStatus(StatusDto.ACTIVE);
    dto.setSeed("testSeed");
    dto.setUserId("testUser");
    dto.setSubordinateUserId("subUser");

    UserDetailsEntity entity = new UserDetailsEntity(dto);

    assertEquals(StatusDto.ACTIVE, entity.getStatus());
    assertEquals("testSeed", entity.getSeed());
    assertEquals("testUser", entity.getUserId());
    assertEquals("subUser", entity.getSubordinateUserId());
    assertNull(entity.getId()); // Since the constructor doesn't set the ID
  }

  @Test
  public void testSettersAndGetters() {
    UserDetailsEntity entity = new UserDetailsEntity();

    entity.setId("123");
    entity.setStatus(StatusDto.BANNED);
    entity.setSeed("abcSeed");
    entity.setUserId("userX");
    entity.setSubordinateUserId("subX");

    assertEquals("123", entity.getId());
    assertEquals(StatusDto.BANNED, entity.getStatus());
    assertEquals("abcSeed", entity.getSeed());
    assertEquals("userX", entity.getUserId());
    assertEquals("subX", entity.getSubordinateUserId());
  }
}
