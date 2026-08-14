package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AliasesResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    AliasesResponseDto dto = new AliasesResponseDto();
    assertNull(dto.getAliases());
    assertNull(dto.getCurrencies());
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    AliasesResponseDto dto = new AliasesResponseDto("Aliases failed");
    assertEquals("Aliases failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    AliasesResponseDto dto = new AliasesResponseDto();
    List<AliasDto> aliases = Collections.singletonList(new AliasDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());

    dto.setAliases(aliases);
    dto.setCurrencies(currencies);

    assertEquals(aliases, dto.getAliases());
    assertEquals(currencies, dto.getCurrencies());
  }
}
