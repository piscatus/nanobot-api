package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaderboardsResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    LeaderboardsResponseDto dto = new LeaderboardsResponseDto();
    assertNull(dto.getCurrencies());
    assertNull(dto.getLeaderboards());
    assertNull(dto.getCreatures());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    LeaderboardsResponseDto dto = new LeaderboardsResponseDto("Leaderboards failed");
    assertEquals("Leaderboards failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    LeaderboardsResponseDto dto = new LeaderboardsResponseDto();
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    List<LeaderboardDto> leaderboards = Collections.singletonList(new LeaderboardDto());
    List<CreatureDto> creatures = Collections.singletonList(new CreatureDto());

    dto.setCurrencies(currencies);
    dto.setLeaderboards(leaderboards);
    dto.setCreatures(creatures);

    assertEquals(currencies, dto.getCurrencies());
    assertEquals(leaderboards, dto.getLeaderboards());
    assertEquals(creatures, dto.getCreatures());
  }
}
