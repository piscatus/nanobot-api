package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.ServerResponseDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class ServerServicesImpl implements ServerServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final GuildWalletsService guildWalletsService;

  public ServerServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    GuildWalletsService guildWalletsService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.guildWalletsService = guildWalletsService;
  }

  @Override
  public ServerResponseDto configurations(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_SERVER, requestDto);

      ServerResponseDto serverResponseDto = new ServerResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          serverResponseDto::setGuildConfigurations
        )
      ) {
        return serverResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          serverResponseDto::setUserDetails,
          true
        )
      ) {
        return serverResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_SERVER,
          requestDto.getUserId(),
          serverResponseDto::setCommands
        )
      ) {
        return serverResponseDto;
      }

      return serverResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_SERVER, e);
      return new ServerResponseDto(Constants.unknownError);
    }
  }

  @Override
  public ServerResponseDto reserves(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_RESERVES, requestDto);

      ServerResponseDto serverResponseDto = new ServerResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          serverResponseDto::setGuildConfigurations
        )
      ) {
        return serverResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          serverResponseDto::setUserDetails,
          true
        )
      ) {
        return serverResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_RESERVES,
          requestDto.getUserId(),
          serverResponseDto::setCommands
        )
      ) {
        return serverResponseDto;
      }

      currenciesService.setCurrencies(serverResponseDto::setCurrencies);

      guildWalletsService.setGuildWallets(
        requestDto.getGuildId(),
        serverResponseDto::setGuildWallets
      );

      return serverResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_SERVER, e);
      return new ServerResponseDto(Constants.unknownError);
    }
  }
}
