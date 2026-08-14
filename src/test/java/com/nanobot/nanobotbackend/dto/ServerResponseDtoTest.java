package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServerResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    ServerResponseDto dto = new ServerResponseDto();
    assertNull(dto.getCurrencies());
    assertNull(dto.getGuildWallets());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    ServerResponseDto dto = new ServerResponseDto("Server failed");
    assertEquals("Server failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    ServerResponseDto dto = new ServerResponseDto();
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    List<WalletDto> guildWallets = Collections.singletonList(new WalletDto());

    dto.setCurrencies(currencies);
    dto.setGuildWallets(guildWallets);

    assertEquals(currencies, dto.getCurrencies());
    assertEquals(guildWallets, dto.getGuildWallets());
  }
}
