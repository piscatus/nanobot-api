package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.CurrenciesResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.HelpResponseDto;
import com.nanobot.nanobotbackend.dto.ReceiveResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrenciesServicesImplTest {

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private CurrenciesService currenciesService;

  private CoreServices coreServices;
  private CurrenciesServicesImpl currenciesServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    currenciesServices =
      new CurrenciesServicesImpl(coreServices, currenciesService);
  }

  @Test
  void currenciesReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    CurrenciesResponseDto result = currenciesServices.currencies(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void currenciesReturnsEarlyWhenUserDetailsFail() {
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

    CurrenciesResponseDto result = currenciesServices.currencies(request);

    assertNotNull(result);
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void currenciesReturnsEarlyWhenCommandsFail() {
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
        eq(Constants.COMMAND_NAME_CURRENCIES),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(false);

    CurrenciesResponseDto result = currenciesServices.currencies(request);

    assertNotNull(result);
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void currenciesDelegatesToCurrenciesServiceWhenAllSucceed() {
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
        eq(Constants.COMMAND_NAME_CURRENCIES),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(true);

    CurrenciesResponseDto result = currenciesServices.currencies(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService).setCurrencies(any());
  }

  @Test
  void currenciesReturnsErrorResponseOnException() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenThrow(new RuntimeException("DB error"));

    CurrenciesResponseDto result = currenciesServices.currencies(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("unknown") || result.getErrorMessage().contains("error"),
      "Expected error message: " + result.getErrorMessage()
    );
  }

  @Test
  void helpReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    HelpResponseDto result = currenciesServices.help(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void helpDelegatesToCurrenciesServiceWhenAllSucceed() {
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
        eq(Constants.COMMAND_NAME_HELP),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(true);

    HelpResponseDto result = currenciesServices.help(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService).setCurrencies(any());
  }

  @Test
  void receiveReturnsEarlyWhenUserDetailsFail() {
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
        eq(false)
      )
    )
      .thenReturn(false);

    ReceiveResponseDto result = currenciesServices.receive(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService, never()).setCurrencies(any());
  }

  @Test
  void receiveDelegatesToCurrenciesAndGenerateAddressesWhenAllSucceed() {
    RequestDto request = minimalRequest();
    UserDetailsDto userDetails = new UserDetailsDto();
    userDetails.setUserId(request.getUserId());
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
        eq(false)
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(userDetails);
          return true;
        }
      );
    when(
      commandsService.setCommands(
        eq(Constants.COMMAND_NAME_RECEIVE),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(true);
    doAnswer(
      inv -> {
        inv.getArgument(0, java.util.function.Consumer.class).accept(List.of());
        return null;
      }
    )
      .when(currenciesService).setCurrencies(any());
    when(userDetailsService.generateAddresses(any(), any())).thenReturn(List.of());

    ReceiveResponseDto result = currenciesServices.receive(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(currenciesService).setCurrencies(any());
    verify(userDetailsService).generateAddresses(any(), any());
  }

  @Test
  void receiveReturnsErrorResponseOnException() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenThrow(new RuntimeException("DB error"));

    ReceiveResponseDto result = currenciesServices.receive(request);

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
