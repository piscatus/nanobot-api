package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.UserWalletsResponseDto;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServicesImplTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private UserWalletsService userWalletsService;

  private CoreServices coreServices;
  private WalletServicesImpl walletServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    walletServices =
      new WalletServicesImpl(
        coreServices,
        currenciesService,
        userWalletsService
      );
  }

  private RequestDto request(String guildId, String userId) {
    return new RequestDto(
      null,
      null,
      false,
      null,
      null,
      false,
      guildId,
      null,
      null,
      null,
      null,
      null,
      userId,
      null,
      null,
      null
    );
  }

  @Test
  void walletShouldReturnEarlyWhenGuildConfigurationsNotSet() {
    RequestDto request = request("g1", "u1");
    when(guildConfigurationsService.setGuildConfigurations(eq("g1"), any()))
      .thenReturn(false);

    UserWalletsResponseDto result = walletServices.wallet(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildConfigurationsService).setGuildConfigurations(eq("g1"), any());
    verify(userWalletsService, org.mockito.Mockito.never())
      .setUserWallets(any(), any());
  }

  @Test
  void walletShouldReturnEarlyWhenUserDetailsNotSet() {
    RequestDto request = request("g1", "u1");
    when(guildConfigurationsService.setGuildConfigurations(eq("g1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("u1"), any(), eq(true)))
      .thenReturn(false);

    UserWalletsResponseDto result = walletServices.wallet(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(userDetailsService).setUserDetails(eq("u1"), any(), eq(true));
    verify(userWalletsService, org.mockito.Mockito.never())
      .setUserWallets(any(), any());
  }

  @Test
  void walletShouldReturnEarlyWhenCommandsNotSet() {
    RequestDto request = request("g1", "u1");
    when(guildConfigurationsService.setGuildConfigurations(eq("g1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("u1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(eq("wallet"), eq("u1"), any()))
      .thenReturn(false);

    UserWalletsResponseDto result = walletServices.wallet(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(commandsService).setCommands(eq("wallet"), eq("u1"), any());
    verify(userWalletsService, org.mockito.Mockito.never())
      .setUserWallets(any(), any());
  }

  @Test
  void walletShouldCallUserWalletsAndCurrenciesWhenAllStepsSucceed() {
    RequestDto request = request("g1", "u1");
    when(guildConfigurationsService.setGuildConfigurations(eq("g1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("u1"), any(), eq(true)))
      .thenAnswer(
        inv -> {
          Consumer<UserDetailsDto> consumer = inv.getArgument(1);
          UserDetailsDto dto = new UserDetailsDto();
          dto.setUserId("u1");
          dto.setSubordinateUserId("u1-sub"); // avoid NPE when fetching subordinate wallets
          consumer.accept(dto);
          return true;
        }
      );
    when(commandsService.setCommands(eq("wallet"), eq("u1"), any()))
      .thenReturn(true);

    UserWalletsResponseDto result = walletServices.wallet(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(userWalletsService).setUserWallets(eq("u1"), any());
    verify(userWalletsService).setUserWallets(eq("u1-sub"), any());
    verify(currenciesService).setCurrencies(any());
  }

  @Test
  void walletShouldReturnErrorResponseWhenExceptionThrown() {
    RequestDto request = request("g1", "u1");
    when(guildConfigurationsService.setGuildConfigurations(eq("g1"), any()))
      .thenReturn(true);
    when(userDetailsService.setUserDetails(eq("u1"), any(), eq(true)))
      .thenReturn(true);
    when(commandsService.setCommands(eq("wallet"), eq("u1"), any()))
      .thenReturn(true);
    doThrow(new RuntimeException("DB error"))
      .when(userWalletsService)
      .setUserWallets(eq("u1"), any());

    UserWalletsResponseDto result = walletServices.wallet(request);

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("unknown error"));
  }
}
