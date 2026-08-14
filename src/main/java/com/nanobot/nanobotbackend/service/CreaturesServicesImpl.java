package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CreaturesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class CreaturesServicesImpl implements CreaturesServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final CreaturesService creaturesService;

  public CreaturesServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    CreaturesService creaturesService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.creaturesService = creaturesService;
  }

  @Override
  public CreaturesResponseDto creatures(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_CREATURES, requestDto);

      CreaturesResponseDto creaturesResponseDto = new CreaturesResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          creaturesResponseDto::setGuildConfigurations
        )
      ) {
        return creaturesResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          creaturesResponseDto::setUserDetails,
          true
        )
      ) {
        return creaturesResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_CREATURES,
          requestDto.getUserId(),
          creaturesResponseDto::setCommands
        )
      ) {
        return creaturesResponseDto;
      }

      currenciesService.setCurrencies(creaturesResponseDto::setCurrencies);

      creaturesService.setCreatures(creaturesResponseDto::setCreatures);

      return creaturesResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_CREATURES, e);
      return new CreaturesResponseDto(Constants.unknownError);
    }
  }
}
