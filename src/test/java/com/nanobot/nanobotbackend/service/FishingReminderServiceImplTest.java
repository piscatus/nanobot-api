package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.FishingReminderResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FishingReminderServiceImplTest {

  @Mock
  private AnglersService anglersService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  private FishingReminderServiceImpl fishingReminderService;

  @BeforeEach
  void setUp() {
    fishingReminderService =
      new FishingReminderServiceImpl(
        anglersService,
        commandsService,
        guildConfigurationsService
      );
  }

  @Test
  void getFishingReminderShouldReturnAggregatedResponse() {
    CommandEntity cmdEntity = new CommandEntity();
    cmdEntity.setId("cmd1");
    cmdEntity.setName("fish");
    cmdEntity.setCommandId("fish-cmd");
    when(commandsService.getCommands(isNull()))
      .thenReturn(List.of(cmdEntity));

    GuildConfigurationsEntity guildEntity = new GuildConfigurationsEntity();
    guildEntity.setId("g1");
    guildEntity.setGuildId("guild1");
    when(guildConfigurationsService.getGuildConfigurations(isNull()))
      .thenReturn(List.of(guildEntity));

    AnglerEntity anglerEntity = new AnglerEntity(
      "guild1",
      "user1",
      true,
      new Date()
    );
    anglerEntity.setId("a1");
    when(anglersService.getRestingAnglers())
      .thenReturn(List.of(anglerEntity));

    FishingReminderResponseDto result =
      fishingReminderService.getFishingReminder();

    assertNotNull(result);
    assertNotNull(result.getCommands());
    assertEquals(1, result.getCommands().size());
    assertEquals("fish", result.getCommands().get(0).getName());
    assertNotNull(result.getAllGuildConfigurations());
    assertEquals(1, result.getAllGuildConfigurations().size());
    assertEquals("guild1", result.getAllGuildConfigurations().get(0).getGuildId());
    assertNotNull(result.getAnglers());
    assertEquals(1, result.getAnglers().size());
    assertEquals("guild1", result.getAnglers().get(0).getGuildId());
    assertEquals("user1", result.getAnglers().get(0).getUserId());

    verify(commandsService).getCommands(isNull());
    verify(guildConfigurationsService).getGuildConfigurations(isNull());
    verify(anglersService).getRestingAnglers();
  }

  @Test
  void getFishingReminderShouldReturnEmptyListsWhenNoData() {
    when(commandsService.getCommands(isNull()))
      .thenReturn(Collections.emptyList());
    when(guildConfigurationsService.getGuildConfigurations(isNull()))
      .thenReturn(Collections.emptyList());
    when(anglersService.getRestingAnglers())
      .thenReturn(Collections.emptyList());

    FishingReminderResponseDto result =
      fishingReminderService.getFishingReminder();

    assertNotNull(result);
    assertNotNull(result.getCommands());
    assertEquals(0, result.getCommands().size());
    assertNotNull(result.getAllGuildConfigurations());
    assertEquals(0, result.getAllGuildConfigurations().size());
    assertNotNull(result.getAnglers());
    assertEquals(0, result.getAnglers().size());
  }

  @Test
  void getFishingReminderShouldPropagateExceptionWhenCommandsServiceFails() {
    when(commandsService.getCommands(isNull()))
      .thenThrow(new RuntimeException("DB error"));

    assertThrows(
      RuntimeException.class,
      () -> fishingReminderService.getFishingReminder()
    );
    verify(commandsService).getCommands(isNull());
  }

  @Test
  void getFishingReminderShouldPropagateExceptionWhenGuildConfigServiceFails() {
    when(commandsService.getCommands(isNull()))
      .thenReturn(Collections.emptyList());
    when(guildConfigurationsService.getGuildConfigurations(isNull()))
      .thenThrow(new RuntimeException("Config error"));

    assertThrows(
      RuntimeException.class,
      () -> fishingReminderService.getFishingReminder()
    );
    verify(commandsService).getCommands(isNull());
    verify(guildConfigurationsService).getGuildConfigurations(isNull());
  }

  @Test
  void getFishingReminderShouldPropagateExceptionWhenAnglersServiceFails() {
    when(commandsService.getCommands(isNull()))
      .thenReturn(Collections.emptyList());
    when(guildConfigurationsService.getGuildConfigurations(isNull()))
      .thenReturn(Collections.emptyList());
    when(anglersService.getRestingAnglers())
      .thenThrow(new RuntimeException("Anglers error"));

    assertThrows(
      RuntimeException.class,
      () -> fishingReminderService.getFishingReminder()
    );
    verify(anglersService).getRestingAnglers();
  }
}
