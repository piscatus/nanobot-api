package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.ServerResponseDto;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerServicesImplTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private GuildWalletsService guildWalletsService;

  private ServerServicesImpl serverServices;

  @BeforeEach
  void setUp() {
    CoreServices coreServices =
      new CoreServices(commandsService, guildConfigurationsService, userDetailsService);
    serverServices = new ServerServicesImpl(coreServices, currenciesService, guildWalletsService);
  }

  @Test
  void configurationsShouldReturnResponseWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    ServerResponseDto result = serverServices.configurations(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(userDetailsService).setUserDetails(eq("user1"), any(), eq(true));
    verify(commandsService).setCommands(any(), eq("user1"), any());
  }

  @Test
  void configurationsShouldReturnEarlyWhenGuildConfigurationsFails() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(false);

    ServerResponseDto result = serverServices.configurations(request);

    assertNotNull(result);
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(userDetailsService, never()).setUserDetails(any(), any(), anyBoolean());
  }

  @Test
  void reservesShouldReturnResponseWithCurrenciesAndGuildWalletsWhenAllSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    ServerResponseDto result = serverServices.reserves(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(currenciesService).setCurrencies(any());
    verify(guildWalletsService).setGuildWallets(eq("guild1"), any());
  }

  @Test
  void reservesShouldReturnEarlyWhenGuildConfigurationsFails() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(false);

    ServerResponseDto result = serverServices.reserves(request);

    assertNotNull(result);
    verify(currenciesService, never()).setCurrencies(any());
    verify(guildWalletsService, never()).setGuildWallets(any(), any());
  }

  @Test
  void configurationsShouldReturnErrorWhenExceptionThrown() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenThrow(new RuntimeException("DB error"));

    ServerResponseDto result = serverServices.configurations(request);

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("unknown error"));
  }
}
