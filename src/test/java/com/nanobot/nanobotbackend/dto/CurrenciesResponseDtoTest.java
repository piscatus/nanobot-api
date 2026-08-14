package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrenciesResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    CurrenciesResponseDto dto = new CurrenciesResponseDto();
    assertNull(dto.getCurrencies());
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    CurrenciesResponseDto dto = new CurrenciesResponseDto("Currencies failed");
    assertEquals("Currencies failed", dto.getErrorMessage());
  }

  @Test
  void setCurrenciesUpdatesField() {
    CurrenciesResponseDto dto = new CurrenciesResponseDto();
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());

    dto.setCurrencies(currencies);

    assertEquals(currencies, dto.getCurrencies());
  }
}
