package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BonusesDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserItemsResponseDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class InventoryServicesImpl implements InventoryServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final CreaturesService creaturesService;

  public final UserItemsService userItemsService;

  public InventoryServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    CreaturesService creaturesService,
    UserItemsService userItemsService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.creaturesService = creaturesService;
    this.userItemsService = userItemsService;
  }

  @Override
  public UserItemsResponseDto inventory(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_INVENTORY, requestDto);

      UserItemsResponseDto userItemsResponseDto = new UserItemsResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          userItemsResponseDto::setGuildConfigurations
        )
      ) {
        return userItemsResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          userItemsResponseDto::setUserDetails,
          true
        )
      ) {
        return userItemsResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_INVENTORY,
          requestDto.getUserId(),
          userItemsResponseDto::setCommands
        )
      ) {
        return userItemsResponseDto;
      }

      userItemsService.setUserItems(
        requestDto.getUserId(),
        userItemsResponseDto::setUserItems
      );

      userItemsService.setUserItems(
        userItemsResponseDto.getUserDetails().getSubordinateUserId(),
        userItemsResponseDto::setSubordinateItems
      );

      currenciesService.setCurrencies(userItemsResponseDto::setCurrencies);

      creaturesService.setCreatures(userItemsResponseDto::setCreatures);

      userItemsResponseDto.setBonuses(new BonusesDto().getBonuses());

      return userItemsResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_INVENTORY, e);
      return new UserItemsResponseDto(Constants.unknownError);
    }
  }
}
