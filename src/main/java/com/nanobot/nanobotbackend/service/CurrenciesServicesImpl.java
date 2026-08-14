package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrenciesResponseDto;
import com.nanobot.nanobotbackend.dto.HelpResponseDto;
import com.nanobot.nanobotbackend.dto.ReceiveResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class CurrenciesServicesImpl implements CurrenciesServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public CurrenciesServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
  }

  @Override
  public CurrenciesResponseDto currencies(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_CURRENCIES, requestDto);

      CurrenciesResponseDto currenciesResponseDto = new CurrenciesResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          currenciesResponseDto::setGuildConfigurations
        )
      ) {
        return currenciesResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          currenciesResponseDto::setUserDetails,
          true
        )
      ) {
        return currenciesResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_CURRENCIES,
          requestDto.getUserId(),
          currenciesResponseDto::setCommands
        )
      ) {
        return currenciesResponseDto;
      }

      currenciesService.setCurrencies(currenciesResponseDto::setCurrencies);

      return currenciesResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_CURRENCIES, e);
      return new CurrenciesResponseDto(Constants.unknownError);
    }
  }

  @Override
  public HelpResponseDto help(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_HELP, requestDto);

      HelpResponseDto helpResponseDto = new HelpResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          helpResponseDto::setGuildConfigurations
        )
      ) {
        return helpResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          helpResponseDto::setUserDetails,
          true
        )
      ) {
        return helpResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_HELP,
          requestDto.getUserId(),
          helpResponseDto::setCommands
        )
      ) {
        return helpResponseDto;
      }

      currenciesService.setCurrencies(helpResponseDto::setCurrencies);

      return helpResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_HELP, e);
      return new HelpResponseDto(Constants.unknownError);
    }
  }

  @Override
  public ReceiveResponseDto receive(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_RECEIVE, requestDto);

      ReceiveResponseDto responseDto = new ReceiveResponseDto();

      coreServices.guildConfigurationsService.setGuildConfigurations(
        requestDto.getGuildId(),
        responseDto::setGuildConfigurations
      );

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          responseDto::setUserDetails,
          false
        )
      ) {
        return responseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_RECEIVE,
          requestDto.getUserId(),
          responseDto::setCommands
        )
      ) {
        return responseDto;
      }

      currenciesService.setCurrencies(responseDto::setCurrencies);

      responseDto.setAddresses(
        coreServices.userDetailsService.generateAddresses(
          responseDto.getCurrencies(),
          responseDto.getUserDetails()
        )
      );

      responseDto.getUserDetails().removeSensitiveData();

      return responseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_RECEIVE, e);
      return new ReceiveResponseDto(Constants.unknownError);
    }
  }
}
