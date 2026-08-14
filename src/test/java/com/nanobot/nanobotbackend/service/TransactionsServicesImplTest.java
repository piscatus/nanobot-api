package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransactionsResponseDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionsServicesImplTest {

  @Mock
  private CoreServices coreServices;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private TransactionsService transactionsService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CommandsService commandsService;

  private TransactionsServicesImpl transactionsServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(commandsService, guildConfigurationsService, userDetailsService);
    transactionsServices =
      new TransactionsServicesImpl(
        coreServices,
        currenciesService,
        transactionsService,
        creaturesService
      );
  }

  @Test
  void transactionsReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    TransactionsResponseDto result = transactionsServices.transactions(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(transactionsService, never()).getTransactions(any());
  }

  @Test
  void transactionsReturnsEarlyWhenUserDetailsFail() {
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

    TransactionsResponseDto result = transactionsServices.transactions(request);

    assertNotNull(result);
    verify(transactionsService, never()).getTransactions(any());
  }

  @Test
  void transactionsReturnsEarlyWhenCommandsFail() {
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
        eq(Constants.COMMAND_NAME_TRANSACTIONS),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(false);

    TransactionsResponseDto result = transactionsServices.transactions(request);

    assertNotNull(result);
    verify(transactionsService, never()).getTransactions(any());
  }

  @Test
  void transactionsDelegatesToAllServicesWhenAllSucceed() {
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
        eq(Constants.COMMAND_NAME_TRANSACTIONS),
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
    doAnswer(
      inv -> {
        inv.getArgument(0, java.util.function.Consumer.class).accept(List.of());
        return null;
      }
    )
      .when(creaturesService).setCreatures(any());
    when(transactionsService.getTransactions(request.getUserId()))
      .thenReturn(List.of(new TransactionEntity()));

    TransactionsResponseDto result = transactionsServices.transactions(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    assertNotNull(result.getTransactions());
    assertNotNull(result.getBonuses());
    verify(currenciesService).setCurrencies(any());
    verify(creaturesService).setCreatures(any());
    verify(transactionsService).getTransactions(request.getUserId());
  }

  @Test
  void transactionsReturnsErrorResponseOnException() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenThrow(new RuntimeException("DB error"));

    TransactionsResponseDto result = transactionsServices.transactions(request);

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
