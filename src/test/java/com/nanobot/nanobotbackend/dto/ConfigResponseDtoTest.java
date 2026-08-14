package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    ConfigResponseDto dto = new ConfigResponseDto();
    assertNull(dto.getCurrencies());
    assertNull(dto.getAliasDetails());
    assertNull(dto.getCreatures());
    assertNull(dto.getTransfer());
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    ConfigResponseDto dto = new ConfigResponseDto("Config failed");
    assertEquals("Config failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    ConfigResponseDto dto = new ConfigResponseDto();
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    AliasDto alias = new AliasDto();
    List<CreatureDto> creatures = Collections.singletonList(new CreatureDto());
    TransferDto transfer = new TransferDto();

    dto.setCurrencies(currencies);
    dto.setAliasDetails(alias);
    dto.setCreatures(creatures);
    dto.setTransfer(transfer);

    assertEquals(currencies, dto.getCurrencies());
    assertEquals(alias, dto.getAliasDetails());
    assertEquals(creatures, dto.getCreatures());
    assertEquals(transfer, dto.getTransfer());
  }
}
