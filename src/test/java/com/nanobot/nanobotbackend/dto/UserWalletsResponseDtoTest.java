package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserWalletsResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    UserWalletsResponseDto dto = new UserWalletsResponseDto();
    assertNull(dto.getUserWallets());
    assertNull(dto.getSubordinateWallets());
    assertNull(dto.getCurrencies());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    UserWalletsResponseDto dto = new UserWalletsResponseDto("User wallets failed");
    assertEquals("User wallets failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    UserWalletsResponseDto dto = new UserWalletsResponseDto();
    List<WalletDto> userWallets = Collections.singletonList(new WalletDto());
    List<WalletDto> subordinateWallets = Collections.singletonList(new WalletDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());

    dto.setUserWallets(userWallets);
    dto.setSubordinateWallets(subordinateWallets);
    dto.setCurrencies(currencies);

    assertEquals(userWallets, dto.getUserWallets());
    assertEquals(subordinateWallets, dto.getSubordinateWallets());
    assertEquals(currencies, dto.getCurrencies());
  }
}
