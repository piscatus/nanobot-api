package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendServicesImpl implements SendServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final CreaturesService creaturesService;

  public final QueuesService queuesService;

  public final TransferService transferService;

  @Autowired
  private TransferExecutorService transferExecutorService;

  public SendServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    CreaturesService creaturesService,
    QueuesService queuesService,
    TransferService transferService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.creaturesService = creaturesService;
    this.queuesService = queuesService;
    this.transferService = transferService;
  }

  @Override
  public TransferResponseDto update(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_UPDATE, requestDto);

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
          Constants.COMMAND_NAME_SEND,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      Optional<UserDetailsEntity> botUserDetailsOptional =
        coreServices.userDetailsService.getUserDetailsByUserId(
          System.getenv("BOT_USER_ID")
        );
      if (!botUserDetailsOptional.isPresent()) {
        return new TransferResponseDto(Constants.unknownError);
      }

      boolean isValidAddress = false;
      for (CurrencyDto currencyDto : transferResponseDto.getCurrencies()) {
        if (
          Pattern.matches(currencyDto.getAddress(), requestDto.getAddress())
        ) {
          if (!currencyDto.getProcessWithdrawals()) {
            transferResponseDto.setErrorMessage(
              "### Representative updates are currently disabled for " +
              currencyDto.getName() +
              " (" +
              currencyDto.getTicker() +
              "). Please try again later.\n" +
              "Users can join our Discord server for inquiries and support help:\n" +
              System.getenv("HOME_SERVER_INVITE_URL")
            );
            return transferResponseDto;
          }
          WalletDto wallet = new WalletDto(currencyDto.getTicker(), "0");
          ArrayList<WalletDto> wallets = new ArrayList<>();
          wallets.add(wallet);
          TransferDto updateTransfer = new TransferDto(
            wallets,
            new ArrayList<>()
          );
          transferResponseDto.setPrimaryTransfer(updateTransfer);
          if (requestDto.getConfirmation()) {
            Optional<UserDetailsEntity> userDetailsOptional =
              coreServices.userDetailsService.getUserDetailsByUserId(
                requestDto.getUserId()
              );
            if (!userDetailsOptional.isPresent()) {
              return new TransferResponseDto(Constants.unknownError);
            }

            UserDetailsEntity userDetails = userDetailsOptional.get();
            String userAddress = CryptoUtil.deriveAddress(
              userDetails,
              currencyDto.getTicker()
            );
            isValidAddress = true;
            QueueDto queueDto = new QueueDto(
              requestDto.getUserId(),
              userAddress,
              requestDto.getAddress(),
              LevelDto.UPDATE,
              null,
              "0",
              currencyDto.getTicker(),
              false,
              userDetails.getSeed(),
              new Date(),
              null
            );
            CryptoUtil.applySigningMaterial(queueDto, userDetails);

            QueueEntity queueEntity = queuesService.createQueue(queueDto);
            // Check that it was added successfully
          } else {
            transferResponseDto.setConfirmation(true);
            return transferResponseDto;
          }
        }
      }
      if (!isValidAddress) {
        transferResponseDto.setErrorMessage(
          "Invalid Address: Please specify a valid address."
        );
        return transferResponseDto;
      }
      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_SEND, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }

  @Override
  public TransferResponseDto send(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_SEND, requestDto);

      TransferResponseDto transferResponseDto = new TransferResponseDto();

      transferResponseDto.setInput(requestDto.getInput());

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
          Constants.COMMAND_NAME_SEND,
          requestDto.getUserId(),
          transferResponseDto::setCommands
        )
      ) {
        return transferResponseDto;
      }

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(transferResponseDto::setCurrencies);

      creaturesService.setCreatures(transferResponseDto::setCreatures);

      transferResponseDto.setPrimaryTransfer(
        transferService.processInputs(
          Constants.COMMAND_NAME_SEND,
          transferResponseDto::setErrorMessage,
          commandMap,
          requestDto.getGuildId(),
          requestDto.getUserId(),
          requestDto.getInput(),
          !requestDto.getConfirmation()
        )
      );

      if (transferResponseDto.getErrorMessage() != null) {
        return transferResponseDto;
      }

      if (
        transferResponseDto.getPrimaryTransfer() != null &&
        transferResponseDto.getPrimaryTransfer().getItems().size() > 0
      ) {
        transferResponseDto.setErrorMessage(
          "Invalid Input: Please specify a **number** and a **currency**."
        );
        return transferResponseDto;
      }

      Optional<UserDetailsEntity> botUserDetailsOptional =
        coreServices.userDetailsService.getUserDetailsByUserId(
          System.getenv("BOT_USER_ID")
        );
      if (!botUserDetailsOptional.isPresent()) {
        return new TransferResponseDto(Constants.unknownError);
      }

      UserDetailsEntity botUserDetails = botUserDetailsOptional.get();

      if (
        transferResponseDto.getPrimaryTransfer() != null &&
        transferResponseDto.getPrimaryTransfer().getWallets().size() > 1
      ) {
        transferResponseDto.setErrorMessage(
          "Invalid Input: Please specify only one **number** and **currency**."
        );
        return transferResponseDto;
      } else if (
        transferResponseDto.getPrimaryTransfer() != null &&
        transferResponseDto.getPrimaryTransfer().getWallets().size() == 1
      ) {
        for (CurrencyDto currencyDto : transferResponseDto.getCurrencies()) {
          if (
            transferResponseDto
              .getPrimaryTransfer()
              .getWallets()
              .get(0)
              .getTicker()
              .equalsIgnoreCase(currencyDto.getTicker())
          ) {
            if (
              Pattern.matches(currencyDto.getAddress(), requestDto.getAddress())
            ) {
              if (!currencyDto.getProcessWithdrawals()) {
                transferResponseDto.setErrorMessage(
                  "### Withdrawals are currently disabled for " +
                  currencyDto.getName() +
                  " (" +
                  currencyDto.getTicker() +
                  "). Please try again later.\n" +
                  "Users can join our Discord server for inquiries and support help:\n" +
                  System.getenv("HOME_SERVER_INVITE_URL")
                );
                return transferResponseDto;
              } else if (requestDto.getConfirmation()) {
                TransferResponseDto transfer =
                  transferExecutorService.executeTransfer(
                    Constants.COMMAND_NAME_SEND,
                    requestDto.getGuildId(),
                    requestDto.getChannelId(),
                    requestDto.getUserId(),
                    null,
                    Collections.singletonList("0"),
                    null,
                    transferResponseDto
                  );
                if (transfer.getCompletedPrimaryTransfers() != null) {
                  TransferDto transferDto = transfer
                    .getCompletedPrimaryTransfers()
                    .flatMap(map -> map.values().stream().findFirst())
                    .orElse(null);
                  QueueDto queueDto = new QueueDto(
                    requestDto.getUserId(),
                    CryptoUtil.deriveAddress(
                      botUserDetails,
                      transferDto.getWallets().get(0).getTicker()
                    ),
                    requestDto.getAddress(),
                    LevelDto.SEND,
                    null,
                    transferDto.getWallets().get(0).getRaw(),
                    transferDto.getWallets().get(0).getTicker(),
                    false,
                    botUserDetails.getSeed(),
                    new Date(),
                    transfer.getTransactionId()
                  );
                  CryptoUtil.applySigningMaterial(queueDto, botUserDetails);
                  QueueEntity queueEntity = queuesService.createQueue(queueDto);
                  // Check that it was added successfully

                } else {
                  transferResponseDto.setErrorMessage(
                    transfer.getErrorMessage()
                  );
                  return transferResponseDto;
                }
              } else {
                transferResponseDto.setConfirmation(true);
                return transferResponseDto;
              }
            } else {
              transferResponseDto.setErrorMessage(
                "Invalid Address: Please specify a valid address for " +
                currencyDto.getName() +
                "."
              );
              return transferResponseDto;
            }
          }
        }
      } else {
        transferResponseDto.setErrorMessage(
          "Invalid Input: Please specify a **number** and a **currency**."
        );
        return transferResponseDto;
      }
      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_SEND, e);
      return new TransferResponseDto(Constants.unknownError);
    }
  }
}
