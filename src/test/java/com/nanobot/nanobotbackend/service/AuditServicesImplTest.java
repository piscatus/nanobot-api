package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.AuditResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServicesImplTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private DropsService dropsService;

  @Mock
  private GuildWalletsService guildWalletsService;

  @Mock
  private UserItemsService userItemsService;

  @Mock
  private UserWalletsService userWalletsService;

  private AuditServicesImpl auditServices;

  @BeforeEach
  void setUp() {
    CoreServices coreServices =
      new CoreServices(commandsService, guildConfigurationsService, userDetailsService);
    auditServices =
      new AuditServicesImpl(
        dropsService,
        coreServices,
        creaturesService,
        currenciesService,
        guildWalletsService,
        userItemsService,
        userWalletsService
      );
  }

  @Test
  void auditShouldReturnResponseWithAllDataWhenAllSettersSucceed() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(any(), eq("user1"), any()))
      .thenReturn(true);

    AuditResponseDto result = auditServices.audit(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(currenciesService).setCurrencies(any());
    verify(creaturesService).setCreatures(any());
    verify(dropsService).setDrops(any());
    verify(guildWalletsService).setAllGuildsWallets(any());
    verify(userItemsService).setAllUsersItems(any());
    verify(userWalletsService).setAllUsersWallets(any());
  }

  @Test
  void auditShouldReturnEarlyWhenGuildConfigurationsFails() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(false);

    AuditResponseDto result = auditServices.audit(request);

    assertNotNull(result);
    verify(currenciesService, never()).setCurrencies(any());
    verify(creaturesService, never()).setCreatures(any());
    verify(dropsService, never()).setDrops(any());
  }

  @Test
  void auditShouldReturnErrorWhenExceptionThrown() {
    RequestDto request = new RequestDto(null, null, false, null, null, false, "guild1", null, null, null, null, null, "user1", null, null, null);
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenThrow(new RuntimeException("DB error"));

    AuditResponseDto result = auditServices.audit(request);

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("unknown error"));
  }
}
