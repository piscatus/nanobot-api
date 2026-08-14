package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.LeaderboardsResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderboardsServicesImplTest {

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private LeaderboardsService leaderboardsService;

  @Mock
  private CreaturesService creaturesService;

  private CoreServices coreServices;
  private LeaderboardsServicesImpl leaderboardsServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    leaderboardsServices =
      new LeaderboardsServicesImpl(
        coreServices,
        currenciesService,
        leaderboardsService,
        creaturesService
      );
  }

  @Test
  void leaderboardsReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    LeaderboardsResponseDto result = leaderboardsServices.leaderboards(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(leaderboardsService, never()).getLeaderboards(any(), any());
    verify(creaturesService, never()).setCreatures(any());
  }

  @Test
  void leaderboardsReturnsEarlyWhenUserDetailsFail() {
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

    LeaderboardsResponseDto result = leaderboardsServices.leaderboards(request);

    assertNotNull(result);
    verify(leaderboardsService, never()).getLeaderboards(any(), any());
  }

  @Test
  void leaderboardsReturnsEarlyWhenCommandsFail() {
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
        eq(Constants.COMMAND_NAME_HELP),
        eq(request.getUserId()),
        any()
      )
    )
      .thenReturn(false);

    LeaderboardsResponseDto result = leaderboardsServices.leaderboards(request);

    assertNotNull(result);
    verify(leaderboardsService, never()).getLeaderboards(any(), any());
  }

  @Test
  void leaderboardsDelegatesToServicesAndSetsLeaderboardsWhenAllSucceed() {
    RequestDto request = minimalRequest();
    LeaderboardEntity lbEntity = new LeaderboardEntity();
    lbEntity.setId("lb-1");
    lbEntity.setUserId("u1");

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
    when(leaderboardsService.getLeaderboards(request.getGuildId(), null))
      .thenReturn(List.of(lbEntity));

    LeaderboardsResponseDto result = leaderboardsServices.leaderboards(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    assertNotNull(result.getLeaderboards());
    assertEquals(1, result.getLeaderboards().size());
    assertEquals("u1", result.getLeaderboards().get(0).getUserId());
    verify(currenciesService).setCurrencies(any());
    verify(creaturesService).setCreatures(any());
    verify(leaderboardsService).getLeaderboards(request.getGuildId(), null);
  }

  @Test
  void leaderboardsReturnsErrorResponseOnException() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenThrow(new RuntimeException("DB error"));

    LeaderboardsResponseDto result = leaderboardsServices.leaderboards(request);

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
