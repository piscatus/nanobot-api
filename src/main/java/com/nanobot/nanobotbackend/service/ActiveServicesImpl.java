package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActiveServicesImpl implements ActiveServices {

  public final CoreServices coreServices;

  public final ActivitiesService activitiesService;

  public ActiveServicesImpl(
    CoreServices coreServices,
    ActivitiesService activitiesService
  ) {
    this.coreServices = coreServices;
    this.activitiesService = activitiesService;
  }

  @Override
  public TransferResponseDto active(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_ACTIVE, requestDto);

      TransferResponseDto transferResponseDto = new TransferResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          transferResponseDto::setGuildConfigurations
        )
      ) {
        return transferResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          transferResponseDto::setUserDetails,
          true
        )
      ) {
        return transferResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_ACTIVE,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      activitiesService.setActivities(
        requestDto,
        false,
        transferResponseDto,
        transferResponseDto::setActivities
      );

      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_ACTIVE, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }
}
