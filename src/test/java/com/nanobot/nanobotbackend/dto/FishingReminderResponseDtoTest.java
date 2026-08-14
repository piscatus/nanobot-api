package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FishingReminderResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    FishingReminderResponseDto dto = new FishingReminderResponseDto();
    assertNull(dto.getAnglers());
    assertNull(dto.getAllGuildConfigurations());
  }

  @Test
  void settersUpdateFields() {
    FishingReminderResponseDto dto = new FishingReminderResponseDto();
    List<AnglerDto> anglers = Collections.singletonList(new AnglerDto());
    List<GuildConfigurationsDto> guilds = Collections.singletonList(new GuildConfigurationsDto());

    dto.setAnglers(anglers);
    dto.setAllGuildConfigurations(guilds);

    assertEquals(anglers, dto.getAnglers());
    assertEquals(guilds, dto.getAllGuildConfigurations());
  }
}
