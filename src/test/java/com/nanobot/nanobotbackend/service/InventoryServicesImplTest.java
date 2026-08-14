package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.UserItemsResponseDto;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServicesImplTest {

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

  @Mock
  private UserItemsService userItemsService;

  private CoreServices coreServices;
  private InventoryServicesImpl inventoryServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    inventoryServices =
      new InventoryServicesImpl(
        coreServices,
        currenciesService,
        creaturesService,
        userItemsService
      );
  }

  @Test
  void inventoryReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    UserItemsResponseDto result = inventoryServices.inventory(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(userItemsService, never()).setUserItems(any(), any());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void inventoryReturnsEarlyWhenUserDetailsFail() {
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

    UserItemsResponseDto result = inventoryServices.inventory(request);

    assertNotNull(result);
    verify(userItemsService, never()).setUserItems(any(), any());
  }

  @Test
  void inventoryReturnsEarlyWhenCommandsFail() {
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
        eq(Constants.COMMAND_NAME_INVENTORY),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(false);

    UserItemsResponseDto result = inventoryServices.inventory(request);

    assertNotNull(result);
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void inventoryDelegatesToUserItemsAndCurrenciesWhenAllSucceed() {
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
          UserDetailsDto dto = new UserDetailsDto();
          dto.setSubordinateUserId(null);
          inv.getArgument(1, java.util.function.Consumer.class).accept(dto);
          return true;
        }
      );
    when(
      commandsService.setCommands(
        eq(Constants.COMMAND_NAME_INVENTORY),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(true);

    UserItemsResponseDto result = inventoryServices.inventory(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(userItemsService).setUserItems(eq(request.getUserId()), any());
    verify(currenciesService).setCurrencies(any());
    verify(creaturesService).setCreatures(any());
  }

  @Test
  void inventoryReturnsErrorResponseOnException() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenThrow(new RuntimeException("DB error"));

    UserItemsResponseDto result = inventoryServices.inventory(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("unknown") || result.getErrorMessage().contains("error"),
      "Expected error message: " + result.getErrorMessage()
    );
  }

  private RequestDto minimalRequest() {
    return new RequestDto(
      null,
      null,
      false,
      null,
      null,
      false,
      "guild-1",
      null,
      null,
      null,
      null,
      null,
      "user-1",
      null,
      null,
      null
    );
  }
}
