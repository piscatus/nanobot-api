package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceiveResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    ReceiveResponseDto dto = new ReceiveResponseDto();
    assertNull(dto.getAddresses());
    assertNull(dto.getCurrencies());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    ReceiveResponseDto dto = new ReceiveResponseDto("Receive failed");
    assertEquals("Receive failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    ReceiveResponseDto dto = new ReceiveResponseDto();
    List<WalletDto> addresses = Collections.singletonList(new WalletDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());

    dto.setAddresses(addresses);
    dto.setCurrencies(currencies);

    assertEquals(addresses, dto.getAddresses());
    assertEquals(currencies, dto.getCurrencies());
  }
}
