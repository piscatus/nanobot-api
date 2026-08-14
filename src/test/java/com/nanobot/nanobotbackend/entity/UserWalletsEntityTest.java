package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserWalletsEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    UserWalletsEntity entity = new UserWalletsEntity();
    assertNull(entity.getUserId());
    assertNull(entity.getWallets());
  }

  @Test
  void userIdConstructorCreatesEntityWithEmptyWallets() {
    UserWalletsEntity entity = new UserWalletsEntity("user-123");

    assertEquals("user-123", entity.getUserId());
    assertNotNull(entity.getWallets());
    assertTrue(entity.getWallets().isEmpty());
  }

  @Test
  void fromDtoCopiesAllFields() {
    UserWalletsDto dto = new UserWalletsDto("user-1");
    dto.setId("uw-1");
    WalletDto wallet = new WalletDto();
    wallet.setTicker("NANO");
    wallet.setRaw("100");
    dto.setWallets(List.of(wallet));

    UserWalletsEntity entity = new UserWalletsEntity(dto);

    assertEquals("uw-1", entity.getId());
    assertEquals("user-1", entity.getUserId());
    assertEquals(1, entity.getWallets().size());
    assertEquals("NANO", entity.getWallets().get(0).getTicker());
    assertEquals("100", entity.getWallets().get(0).getRaw());
  }

  @Test
  void fromDtoHandlesNullWallets() {
    UserWalletsDto dto = new UserWalletsDto("user-2");
    dto.setId("uw-2");
    dto.setWallets(null);

    UserWalletsEntity entity = new UserWalletsEntity(dto);

    assertEquals("uw-2", entity.getId());
    assertEquals("user-2", entity.getUserId());
    assertNull(entity.getWallets());
  }

  @Test
  void settersUpdateFields() {
    UserWalletsEntity entity = new UserWalletsEntity();
    entity.setUserId("user-3");
    entity.setWallets(Collections.emptyList());

    assertEquals("user-3", entity.getUserId());
    assertNotNull(entity.getWallets());
    assertTrue(entity.getWallets().isEmpty());
  }
}
