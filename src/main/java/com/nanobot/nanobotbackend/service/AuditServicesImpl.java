package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AuditResponseDto;
import com.nanobot.nanobotbackend.dto.BonusesDto;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuditServicesImpl implements AuditServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final CreaturesService creaturesService;

  public final DropsService dropsService;

  public final GuildWalletsService guildWalletsService;

  public final UserItemsService userItemsService;

  public final UserWalletsService userWalletsService;

  public AuditServicesImpl(
    DropsService dropsService,
    CoreServices coreServices,
    CreaturesService creaturesService,
    CurrenciesService currenciesService,
    GuildWalletsService guildWalletsService,
    UserItemsService userItemsService,
    UserWalletsService userWalletsService
  ) {
    this.dropsService = dropsService;
    this.coreServices = coreServices;
    this.creaturesService = creaturesService;
    this.currenciesService = currenciesService;
    this.guildWalletsService = guildWalletsService;
    this.userItemsService = userItemsService;
    this.userWalletsService = userWalletsService;
  }

  @Override
  public AuditResponseDto audit(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_AUDIT, requestDto);

      AuditResponseDto auditResponseDto = new AuditResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          auditResponseDto::setGuildConfigurations
        )
      ) {
        return auditResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          auditResponseDto::setUserDetails,
          true
        )
      ) {
        return auditResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_AUDIT,
          requestDto.getUserId(),
          auditResponseDto::setCommands
        )
      ) {
        return auditResponseDto;
      }

      currenciesService.setCurrencies(auditResponseDto::setCurrencies);

      creaturesService.setCreatures(auditResponseDto::setCreatures);

      auditResponseDto.setBonuses(new BonusesDto().getBonuses());

      dropsService.setDrops(auditResponseDto::setDrops);

      guildWalletsService.setAllGuildsWallets(
        auditResponseDto::setGuildsWallets
      );

      userItemsService.setAllUsersItems(auditResponseDto::setUsersItems);

      userWalletsService.setAllUsersWallets(auditResponseDto::setUsersWallets);

      return auditResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_AUDIT, e);
      return new AuditResponseDto(Constants.unknownError);
    }
  }
}
