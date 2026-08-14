package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.CreaturesResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreaturesServicesImplTest {

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private CreaturesService creaturesService;

  private CoreServices coreServices;
  private CreaturesServicesImpl creaturesServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    creaturesServices =
      new CreaturesServicesImpl(coreServices, currenciesService, creaturesService);
  }

  @Test
  void creaturesReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    CreaturesResponseDto result = creaturesServices.creatures(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(creaturesService, never()).setCreatures(any());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void creaturesReturnsEarlyWhenUserDetailsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(true);
    when(
      userDetailsService.setUserDetails(
        eq(request.getUserId()),
        any(),
        eq(true)
      )
    )
      .thenReturn(false);

    CreaturesResponseDto result = creaturesServices.creatures(request);

    assertNotNull(result);
    verify(creaturesService, never()).setCreatures(any());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void creaturesReturnsEarlyWhenCommandsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(true);
    when(
      userDetailsService.setUserDetails(
        eq(request.getUserId()),
        any(),
        eq(true)
      )
    )
      .thenReturn(true);
    when(
      commandsService.setCommands(
        eq(Constants.COMMAND_NAME_CREATURES),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(false);

    CreaturesResponseDto result = creaturesServices.creatures(request);

    assertNotNull(result);
    verify(creaturesService, never()).setCreatures(any());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void creaturesDelegatesToServicesWhenAllSucceed() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(new GuildConfigurationsDto());
          return true;
        }
      );
    when(
      userDetailsService.setUserDetails(
        eq(request.getUserId()),
        any(),
        eq(true)
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(new UserDetailsDto());
          return true;
        }
      );
    when(
      commandsService.setCommands(
        eq(Constants.COMMAND_NAME_CREATURES),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(true);

    CreaturesResponseDto result = creaturesServices.creatures(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService).setCurrencies(any());
    verify(creaturesService).setCreatures(any());
  }

  @Test
  void creaturesReturnsErrorResponseOnException() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenThrow(new RuntimeException("DB error"));

    CreaturesResponseDto result = creaturesServices.creatures(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("unknown") || result.getErrorMessage().contains("error"),
      "Expected error message: " + result.getErrorMessage()
    );
  }

  private RequestDto minimalRequest() {
    RequestDto dto = new RequestDto(
      null,
      null,
      false,
      null,
      null,
      false,
      "guild1",
      null,
      null,
      null,
      null,
      null,
      "user1",
      null,
      null,
      null
    );
    dto.setId("req-1");
    return dto;
  }
}
