package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BonusesDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.dto.TransactionsResponseDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TransactionsServicesImpl implements TransactionsServices {

  public final CoreServices coreServices;

  public final CurrenciesService currenciesService;

  public final TransactionsService transactionsService;

  public final CreaturesService creaturesService;

  public TransactionsServicesImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    TransactionsService transactionsService,
    CreaturesService creaturesService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.transactionsService = transactionsService;
    this.creaturesService = creaturesService;
  }

  @Override
  public TransactionsResponseDto transactions(RequestDto requestDto) {
    try {
      // LoggingUtil.requestLogging(
      //   Constants.COMMAND_NAME_TRANSACTIONS,
      //   requestDto
      // );

      TransactionsResponseDto transactionsResponseDto =
        new TransactionsResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          transactionsResponseDto::setGuildConfigurations
        )
      ) {
        return transactionsResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          transactionsResponseDto::setUserDetails,
          true
        )
      ) {
        return transactionsResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_TRANSACTIONS,
          requestDto.getUserId(),
          transactionsResponseDto::setCommands
        )
      ) {
        return transactionsResponseDto;
      }

      currenciesService.setCurrencies(transactionsResponseDto::setCurrencies);

      creaturesService.setCreatures(transactionsResponseDto::setCreatures);

      transactionsResponseDto.setBonuses(new BonusesDto().getBonuses());

      List<TransactionEntity> transactionEntities =
        transactionsService.getTransactions(requestDto.getUserId());

      List<TransactionDto> transactions = new ArrayList<>();
      for (TransactionEntity transactionEntity : transactionEntities) {
        transactions.add(new TransactionDto(transactionEntity));
      }
      transactionsResponseDto.setTransactions(transactions);

      return transactionsResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_TRANSACTIONS, e);
      return new TransactionsResponseDto(Constants.unknownError);
    }
  }
}
