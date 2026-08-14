package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import com.nanobot.nanobotbackend.dto.LeaderboardsResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardsServicesImpl implements LeaderboardsServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final LeaderboardsService leaderboardsService;

  public final CreaturesService creaturesService;

  public LeaderboardsServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    LeaderboardsService leaderboardsService,
    CreaturesService creaturesService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.leaderboardsService = leaderboardsService;
    this.creaturesService = creaturesService;
  }

  @Override
  public LeaderboardsResponseDto leaderboards(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(
      //   Constants.COMMAND_NAME_LEADERBOARDS,
      //   requestDto
      // );

      LeaderboardsResponseDto leaderboardsResponseDto =
        new LeaderboardsResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          leaderboardsResponseDto::setGuildConfigurations
        )
      ) {
        return leaderboardsResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          leaderboardsResponseDto::setUserDetails,
          true
        )
      ) {
        return leaderboardsResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_HELP,
          requestDto.getUserId(),
          leaderboardsResponseDto::setCommands
        )
      ) {
        return leaderboardsResponseDto;
      }

      currenciesService.setCurrencies(leaderboardsResponseDto::setCurrencies);

      creaturesService.setCreatures(leaderboardsResponseDto::setCreatures);

      List<LeaderboardEntity> leaderboardEntities =
        leaderboardsService.getLeaderboards(requestDto.getGuildId(), null);

      List<LeaderboardDto> leaderboards = new ArrayList<>();
      for (LeaderboardEntity leaderboardEntity : leaderboardEntities) {
        leaderboards.add(new LeaderboardDto(leaderboardEntity));
      }
      leaderboardsResponseDto.setLeaderboards(leaderboards);

      return leaderboardsResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_LEADERBOARDS, e);
      return new LeaderboardsResponseDto(Constants.unknownError);
    }
  }
}
