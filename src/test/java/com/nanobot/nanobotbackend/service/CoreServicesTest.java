package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.BonusesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoreServicesTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  private CoreServices coreServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(commandsService, guildConfigurationsService, userDetailsService);
  }

  @Test
  void bonusesShouldReturnResponseWithBonusesWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    BonusesResponseDto result = coreServices.bonuses(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    assertNotNull(result.getBonuses());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(userDetailsService).setUserDetails(eq("user1"), any(), eq(true));
    verify(commandsService).setCommands(any(), eq("user1"), any());
  }

  @Test
  void bonusesShouldReturnErrorWhenExceptionThrown() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenThrow(new RuntimeException("DB error"));

    BonusesResponseDto result = coreServices.bonuses(request);

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("unknown error"));
  }

  @Test
  void cuteShouldReturnResponseWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    BaseResponseDto result = coreServices.cute(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
  }

  @Test
  void rolesShouldReturnResponseWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    BaseResponseDto result = coreServices.roles(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
  }

  @Test
  void rulesShouldReturnResponseWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    BaseResponseDto result = coreServices.rules(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
  }
}
