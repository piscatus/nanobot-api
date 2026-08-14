package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserWalletsResponseDto;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import org.springframework.stereotype.Service;

@Service
public class WalletServicesImpl implements WalletServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final UserWalletsService userWalletsService;

  public WalletServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    UserWalletsService userWalletsService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.userWalletsService = userWalletsService;
  }

  @Override
  public UserWalletsResponseDto wallet(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(Constants.COMMAND_NAME_WALLET, requestDto);

      UserWalletsResponseDto walletResponseDto = new UserWalletsResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          walletResponseDto::setGuildConfigurations
        )
      ) {
        return walletResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          walletResponseDto::setUserDetails,
          true
        )
      ) {
        return walletResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_WALLET,
          requestDto.getUserId(),
          walletResponseDto::setCommands
        )
      ) {
        return walletResponseDto;
      }

      userWalletsService.setUserWallets(
        requestDto.getUserId(),
        walletResponseDto::setUserWallets
      );

      userWalletsService.setUserWallets(
        walletResponseDto.getUserDetails().getSubordinateUserId(),
        walletResponseDto::setSubordinateWallets
      );

      currenciesService.setCurrencies(walletResponseDto::setCurrencies);

      return walletResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_WALLET, e);
      return new UserWalletsResponseDto(Constants.unknownError);
    }
  }
}
