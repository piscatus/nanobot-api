package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.AliasesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AliasesServicesImpl implements AliasesServices {

  public final CoreServices coreServices;

  public final AliasesService aliasesService;

  public final CurrenciesService currenciesService;

  public AliasesServicesImpl(
    CoreServices coreServices,
    AliasesService aliasesService,
    CurrenciesService currenciesService
  ) {
    this.coreServices = coreServices;
    this.aliasesService = aliasesService;
    this.currenciesService = currenciesService;
  }

  @Override
  public AliasesResponseDto aliases(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_ALIASES, requestDto);

      AliasesResponseDto aliasesResponseDto = new AliasesResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          aliasesResponseDto::setGuildConfigurations
        )
      ) {
        return aliasesResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          aliasesResponseDto::setUserDetails,
          true
        )
      ) {
        return aliasesResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_ALIASES,
          requestDto.getUserId(),
          aliasesResponseDto::setCommands
        )
      ) {
        return aliasesResponseDto;
      }

      currenciesService.setCurrencies(aliasesResponseDto::setCurrencies);

      aliasesService.setAliases(
        aliasesResponseDto::setAliases,
        !requestDto.getGlobal() &&
          StringUtil.isValidString(requestDto.getGuildId())
          ? requestDto.getGuildId()
          : "GLOBAL"
      );

      return aliasesResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_ALIASES, e);
      return new AliasesResponseDto(Constants.unknownError);
    }
  }
}
