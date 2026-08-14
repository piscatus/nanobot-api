package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveServicesImplTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private ActivitiesService activitiesService;

  private ActiveServicesImpl activeServices;

  @BeforeEach
  void setUp() {
    CoreServices coreServices =
      new CoreServices(commandsService, guildConfigurationsService, userDetailsService);
    activeServices = new ActiveServicesImpl(coreServices, activitiesService);
  }

  @Test
  void activeShouldReturnResponseWithActivitiesWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    TransferResponseDto result = activeServices.active(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(activitiesService).setActivities(eq(request), eq(false), any(), any());
  }

  @Test
  void activeShouldReturnEarlyWhenGuildConfigurationsFails() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(false);

    TransferResponseDto result = activeServices.active(request);

    assertNotNull(result);
    verify(activitiesService, never()).setActivities(any(), anyBoolean(), any(), any());
  }

  @Test
  void activeShouldReturnErrorWhenExceptionThrown() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenThrow(new RuntimeException("DB error"));

    TransferResponseDto result = activeServices.active(request);

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("unknown error"));
  }
}
