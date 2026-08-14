package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.BonusesDto;
import com.nanobot.nanobotbackend.dto.BonusesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class CoreServices {

  public final CommandsService commandsService;

  public final GuildConfigurationsService guildConfigurationsService;

  public final UserDetailsService userDetailsService;

  public CoreServices(
    CommandsService commandsService,
    GuildConfigurationsService guildConfigurationsService,
    UserDetailsService userDetailsService
  ) {
    this.commandsService = commandsService;
    this.guildConfigurationsService = guildConfigurationsService;
    this.userDetailsService = userDetailsService;
  }

  public BonusesResponseDto bonuses(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_BONUSES, requestDto);

      BonusesResponseDto bonusesResponseDto = new BonusesResponseDto();

      if (
        !guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          bonusesResponseDto::setGuildConfigurations
        )
      ) {
        return bonusesResponseDto;
      }

      if (
        !userDetailsService.setUserDetails(
          requestDto.getUserId(),
          bonusesResponseDto::setUserDetails,
          true
        )
      ) {
        return bonusesResponseDto;
      }

      if (
        !commandsService.setCommands(
          Constants.COMMAND_NAME_BONUSES,
          requestDto.getUserId(),
          bonusesResponseDto::setCommands
        )
      ) {
        return bonusesResponseDto;
      }

      bonusesResponseDto.setBonuses(new BonusesDto().getBonuses());

      return bonusesResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_BONUSES, e);
      return new BonusesResponseDto(Constants.unknownError);
    }
  }

  public BaseResponseDto cute(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_CUTE, requestDto);

      BaseResponseDto baseResponseDto = new BaseResponseDto();

      if (
        !guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          baseResponseDto::setGuildConfigurations
        )
      ) {
        return baseResponseDto;
      }

      if (
        !userDetailsService.setUserDetails(
          requestDto.getUserId(),
          baseResponseDto::setUserDetails,
          true
        )
      ) {
        return baseResponseDto;
      }

      if (
        !commandsService.setCommands(
          Constants.COMMAND_NAME_CUTE,
          requestDto.getUserId(),
          baseResponseDto::setCommands
        )
      ) {
        return baseResponseDto;
      }

      return baseResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_CUTE, e);
      return new BaseResponseDto(Constants.unknownError);
    }
  }

  public BaseResponseDto roles(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_ROLES, requestDto);

      BaseResponseDto baseResponseDto = new BaseResponseDto();

      if (
        !guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          baseResponseDto::setGuildConfigurations
        )
      ) {
        return baseResponseDto;
      }

      if (
        !userDetailsService.setUserDetails(
          requestDto.getUserId(),
          baseResponseDto::setUserDetails,
          true
        )
      ) {
        return baseResponseDto;
      }

      if (
        !commandsService.setCommands(
          Constants.COMMAND_NAME_ROLES,
          requestDto.getUserId(),
          baseResponseDto::setCommands
        )
      ) {
        return baseResponseDto;
      }

      return baseResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_ROLES, e);
      return new BaseResponseDto(Constants.unknownError);
    }
  }

  public BaseResponseDto rules(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_RULES, requestDto);

      BaseResponseDto baseResponseDto = new BaseResponseDto();

      if (
        !guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          baseResponseDto::setGuildConfigurations
        )
      ) {
        return baseResponseDto;
      }

      if (
        !userDetailsService.setUserDetails(
          requestDto.getUserId(),
          baseResponseDto::setUserDetails,
          true
        )
      ) {
        return baseResponseDto;
      }

      if (
        !commandsService.setCommands(
          Constants.COMMAND_NAME_RULES,
          requestDto.getUserId(),
          baseResponseDto::setCommands
        )
      ) {
        return baseResponseDto;
      }

      return baseResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_RULES, e);
      return new BaseResponseDto(Constants.unknownError);
    }
  }
}
