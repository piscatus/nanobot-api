package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class HelpResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    HelpResponseDto dto = new HelpResponseDto();
    assertNull(dto.getCurrencies());
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    HelpResponseDto dto = new HelpResponseDto("Help failed");
    assertEquals("Help failed", dto.getErrorMessage());
  }

  @Test
  void setCurrenciesUpdatesField() {
    HelpResponseDto dto = new HelpResponseDto();
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());

    dto.setCurrencies(currencies);

    assertEquals(currencies, dto.getCurrencies());
  }
}
