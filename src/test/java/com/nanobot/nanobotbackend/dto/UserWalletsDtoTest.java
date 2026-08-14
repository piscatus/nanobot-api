package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UserWalletsDtoTest {

  @Test
  void testNoArgConstructorAndSetters() {
    UserWalletsDto dto = new UserWalletsDto();
    dto.setUserId("user001");

    List<WalletDto> wallets = new ArrayList<>();
    wallets.add(new WalletDto("XNO", "10000000000000", true));
    wallets.add(new WalletDto("BAN", "0", false));
    dto.setWallets(wallets);

    assertEquals("user001", dto.getUserId());
    assertEquals(wallets, dto.getWallets());
  }

  @Test
  void testConstructorWithUserId() {
    UserWalletsDto dto = new UserWalletsDto("user002");

    assertEquals("user002", dto.getUserId());
    assertTrue(dto.getWallets().isEmpty());
  }

  @Test
  void testConstructorWithUserIdAndWallets() {
    List<WalletDto> wallets = new ArrayList<>();
    wallets.add(new WalletDto("BAN", "123456789"));
    wallets.add(new WalletDto("XNO", "nano_123", "0"));

    UserWalletsDto dto = new UserWalletsDto("user003", wallets);

    assertEquals("user003", dto.getUserId());
    assertEquals(2, dto.getWallets().size());
    assertEquals("BAN", dto.getWallets().get(0).getTicker());
    assertEquals("XNO", dto.getWallets().get(1).getTicker());
    assertEquals("nano_123", dto.getWallets().get(1).getAddress());
  }

  @Test
  void testSetWalletsToNull() {
    UserWalletsDto dto = new UserWalletsDto("user004", null);

    assertEquals("user004", dto.getUserId());
    assertTrue(dto.getWallets().isEmpty());
  }
}
