package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.ConfigResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigServicesImplTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private AliasesService aliasesService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private ProfanitiesService profanitiesService;

  @Mock
  private TransferService transferService;

  private ConfigServicesImpl configServices;

  @BeforeEach
  void setUp() {
    CoreServices coreServices =
      new CoreServices(commandsService, guildConfigurationsService, userDetailsService);
    configServices =
      new ConfigServicesImpl(
        coreServices,
        aliasesService,
        currenciesService,
        creaturesService,
        profanitiesService,
        transferService
      );
  }

  @Test
  void configShouldReturnResponseWhenAllSettersSucceedAndNoAliasData() {
    GuildConfigurationsDto request = new GuildConfigurationsDto();
    request.setGuildId("guild1");
    request.setUserId("user1");
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenAnswer(inv -> {
        GuildConfigurationsDto dto = new GuildConfigurationsDto();
        dto.setStatus(StatusDto.ACTIVE);
        inv.getArgument(1, java.util.function.Consumer.class).accept(dto);
        return true;
      });
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    doAnswer(inv -> {
      inv.getArgument(2, java.util.function.Consumer.class)
        .accept(List.of(new CommandDto("wallet", "w"), new CommandDto("inventory", "i")));
      return true;
    })
      .when(commandsService)
      .setCommands(any(), eq("user1"), any());
    doAnswer(inv -> {
      inv.getArgument(0, java.util.function.Consumer.class).accept(Collections.emptyList());
      return null;
    })
      .when(currenciesService)
      .setCurrencies(any());
    doAnswer(inv -> {
      inv.getArgument(0, java.util.function.Consumer.class).accept(Collections.emptyList());
      return null;
    })
      .when(creaturesService)
      .setCreatures(any());
    GuildConfigurationsEntity existingConfig = new GuildConfigurationsEntity();
    existingConfig.setId("cfg-1");
    existingConfig.setGuildId("guild1");
    existingConfig.setStatus(StatusDto.ACTIVE);
    when(guildConfigurationsService.getGuildConfigurations("guild1"))
      .thenReturn(List.of(existingConfig));
    when(guildConfigurationsService.saveGuildConfigurations(any()))
      .thenAnswer(inv -> inv.getArgument(0));

    ConfigResponseDto result = configServices.config(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("guild1"), any());
    verify(currenciesService).setCurrencies(any());
    verify(creaturesService).setCreatures(any());
  }

  @Test
  void addAliasRejectsConcatenatedValue() {
    GuildConfigurationsDto request = new GuildConfigurationsDto();
    request.setGuildId("guild1");
    request.setUserId("user1");
    AliasDto aliasData = new AliasDto();
    aliasData.setSingular("myalias");
    aliasData.setPlural("myaliases");
    aliasData.setValue("$1 ban + $1 ban");
    aliasData.setEmoji("🍌");
    request.setAliasData(aliasData);

    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenAnswer(inv -> {
        GuildConfigurationsDto dto = new GuildConfigurationsDto();
        dto.setStatus(StatusDto.ACTIVE);
        inv.getArgument(1, java.util.function.Consumer.class).accept(dto);
        return true;
      });
    when(userDetailsService.setUserDetails(eq("user1"), any(), eq(true)))
      .thenReturn(true);
    doAnswer(inv -> {
      inv
        .getArgument(2, java.util.function.Consumer.class)
        .accept(
          List.of(
            new CommandDto("wallet", "w"),
            new CommandDto("inventory", "i")
          )
        );
      return true;
    })
      .when(commandsService)
      .setCommands(any(), eq("user1"), any());
    doAnswer(inv -> {
      inv.getArgument(0, java.util.function.Consumer.class).accept(Collections.emptyList());
      return null;
    })
      .when(currenciesService)
      .setCurrencies(any());
    doAnswer(inv -> {
      inv.getArgument(0, java.util.function.Consumer.class).accept(Collections.emptyList());
      return null;
    })
      .when(creaturesService)
      .setCreatures(any());
    when(aliasesService.getAliasesByEitherGuildId(null, "guild1"))
      .thenReturn(Collections.emptyList());

    ConfigResponseDto result = configServices.config(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("cannot combine"));
    verify(transferService, never())
      .processInputs(any(), any(), any(), any(), any(), any(), anyBoolean());
  }

  @Test
  void configShouldReturnEarlyWhenGuildConfigurationsFails() {
    GuildConfigurationsDto request = new GuildConfigurationsDto();
    request.setGuildId("guild1");
    request.setUserId("user1");
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenReturn(false);

    ConfigResponseDto result = configServices.config(request);

    assertNotNull(result);
    verify(currenciesService, never()).setCurrencies(any());
    verify(creaturesService, never()).setCreatures(any());
  }

  @Test
  void configShouldReturnErrorWhenExceptionThrown() {
    GuildConfigurationsDto request = new GuildConfigurationsDto();
    request.setGuildId("guild1");
    request.setUserId("user1");
    when(guildConfigurationsService.setGuildConfigurations(eq("guild1"), any()))
      .thenThrow(new RuntimeException("DB error"));

    ConfigResponseDto result = configServices.config(request);

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("unknown error"));
  }
}
