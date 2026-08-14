package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.AliasesResponseDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AliasesServicesImplTest {

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

  private AliasesServicesImpl aliasesServices;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    CoreServices coreServices = new CoreServices(
      commandsService,
      guildConfigurationsService,
      userDetailsService
    );
    aliasesServices = new AliasesServicesImpl(
      coreServices,
      aliasesService,
      currenciesService
    );
  }

  @Test
  void aliasesReturnsEarlyWhenGuildConfigCannotBeSet() {
    RequestDto request = request("guild-1", "user-1", false);
    when(guildConfigurationsService.setGuildConfigurations(anyString(), any()))
      .thenReturn(false);

    AliasesResponseDto result = aliasesServices.aliases(request);

    assertNull(result.getErrorMessage());
    assertNull(result.getGuildConfigurations());
    verify(userDetailsService, never()).setUserDetails(anyString(), any(), anyBoolean());
    verify(commandsService, never()).setCommands(anyString(), anyString(), any());
    verify(currenciesService, never()).setCurrencies(any());
    verify(aliasesService, never()).setAliases(any(), anyString());
  }

  @Test
  void aliasesPopulatesResponseAndUsesGuildScopeWhenNotGlobal() {
    RequestDto request = request("guild-42", "user-7", false);
    mockCommonHappyPath();

    doAnswer(invocation -> {
      Consumer<List<CurrencyDto>> setCurrencies = invocation.getArgument(0);
      setCurrencies.accept(List.of(currency("BAN")));
      return null;
    })
      .when(currenciesService).setCurrencies(any());

    doAnswer(invocation -> {
      Consumer<List<AliasDto>> setAliases = invocation.getArgument(0);
      setAliases.accept(List.of(alias("tip")));
      return null;
    })
      .when(aliasesService).setAliases(any(), eq("guild-42"));

    AliasesResponseDto result = aliasesServices.aliases(request);

    assertNull(result.getErrorMessage());
    assertNotNull(result.getGuildConfigurations());
    assertNotNull(result.getUserDetails());
    assertNotNull(result.getCommands());
    assertEquals(1, result.getCurrencies().size());
    assertEquals("BAN", result.getCurrencies().get(0).getTicker());
    assertEquals(1, result.getAliases().size());
    assertEquals("tip", result.getAliases().get(0).getSingular());
    verify(userDetailsService).setUserDetails(eq("user-7"), any(), eq(true));
    verify(aliasesService).setAliases(any(), eq("guild-42"));
  }

  @Test
  void aliasesUsesGlobalScopeWhenGlobalIsTrue() {
    RequestDto request = request("guild-local", "user-9", true);
    mockCommonHappyPath();
    doNothing().when(currenciesService).setCurrencies(any());
    doNothing().when(aliasesService).setAliases(any(), anyString());

    aliasesServices.aliases(request);

    verify(aliasesService).setAliases(any(), eq("GLOBAL"));
  }

  @Test
  void aliasesReturnsUnknownErrorWhenUnexpectedExceptionOccurs() {
    RequestDto request = request("guild-1", "user-1", false);
    when(guildConfigurationsService.setGuildConfigurations(anyString(), any()))
      .thenThrow(new RuntimeException("boom"));

    AliasesResponseDto result = aliasesServices.aliases(request);

    assertEquals(Constants.unknownError, result.getErrorMessage());
  }

  private void mockCommonHappyPath() {
    when(guildConfigurationsService.setGuildConfigurations(anyString(), any()))
      .thenAnswer(invocation -> {
        Consumer<GuildConfigurationsDto> consumer = invocation.getArgument(1);
        consumer.accept(new GuildConfigurationsDto());
        return true;
      });

    when(userDetailsService.setUserDetails(anyString(), any(), eq(true)))
      .thenAnswer(invocation -> {
        Consumer<UserDetailsDto> consumer = invocation.getArgument(1);
        consumer.accept(new UserDetailsDto());
        return true;
      });

    when(commandsService.setCommands(anyString(), anyString(), any()))
      .thenAnswer(invocation -> {
        Consumer<List<CommandDto>> consumer = invocation.getArgument(2);
        consumer.accept(List.of(new CommandDto()));
        return true;
      });
  }

  private RequestDto request(String guildId, String userId, boolean global) {
    return new RequestDto(
      null,
      "channel-1",
      false,
      null,
      null,
      global,
      guildId,
      null,
      null,
      "",
      null,
      null,
      userId,
      null,
      null,
      null
    );
  }

  private AliasDto alias(String singular) {
    AliasDto aliasDto = new AliasDto();
    aliasDto.setSingular(singular);
    return aliasDto;
  }

  private CurrencyDto currency(String ticker) {
    CurrencyDto currencyDto = new CurrencyDto();
    currencyDto.setTicker(ticker);
    return currencyDto;
  }
}
