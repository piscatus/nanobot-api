package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import org.junit.jupiter.api.Test;

class UserDetailsDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    UserDetailsDto dto = new UserDetailsDto();
    assertNull(dto.getUserId());
    assertNull(dto.getSubordinateUserId());
    assertNull(dto.getStatus());
    assertNull(dto.getSeed());
  }

  @Test
  void fullConstructorSetsAllFields() {
    StatusDto status = StatusDto.ACTIVE;
    UserDetailsDto dto = new UserDetailsDto("u1", "sub1", status, "seed123");

    assertEquals("u1", dto.getUserId());
    assertEquals("sub1", dto.getSubordinateUserId());
    assertEquals(status, dto.getStatus());
    assertEquals("seed123", dto.getSeed());
  }

  @Test
  void fromEntityCopiesAllFields() {
    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setId("e-1");
    entity.setUserId("u1");
    entity.setSubordinateUserId("sub1");
    entity.setSeed("secret");
    StatusDto status = StatusDto.ACTIVE;
    entity.setStatus(status);

    UserDetailsDto dto = new UserDetailsDto(entity);

    assertEquals("e-1", dto.getId());
    assertEquals("u1", dto.getUserId());
    assertEquals("sub1", dto.getSubordinateUserId());
    assertEquals("secret", dto.getSeed());
    assertEquals(status, dto.getStatus());
  }

  @Test
  void removeSensitiveDataClearsSeed() {
    UserDetailsDto dto = new UserDetailsDto("u1", "sub1", StatusDto.ACTIVE, "seed");
    assertEquals("seed", dto.getSeed());

    dto.removeSensitiveData();
    assertNull(dto.getSeed());
  }

  @Test
  void settersUpdateFields() {
    UserDetailsDto dto = new UserDetailsDto();
    StatusDto status = StatusDto.ACTIVE;

    dto.setUserId("u2");
    dto.setSubordinateUserId("sub2");
    dto.setStatus(status);
    dto.setSeed("new-seed");

    assertEquals("u2", dto.getUserId());
    assertEquals("sub2", dto.getSubordinateUserId());
    assertEquals(status, dto.getStatus());
    assertEquals("new-seed", dto.getSeed());
  }
}
