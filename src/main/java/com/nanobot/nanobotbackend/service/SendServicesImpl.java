package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.service.chain.ChainAdapter;
import com.nanobot.nanobotbackend.service.chain.ChainAdapterRegistry;
import com.nanobot.nanobotbackend.service.chain.FeeQuote;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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

  @Autowired
  private ChainAdapterRegistry chainAdapterRegistry;

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

  /**
   * Error message when a fee-bearing currency has no usable fee estimate, or
   * null when the withdrawal may proceed.
   *
   * <p>Asks the adapter rather than the currency document, because whether a
   * network charges a fee is a property of the protocol and not something an
   * administrator should be able to get wrong by editing a field.
   */
  private String validateFeeEstimateAvailable(CurrencyDto currencyDto) {
    boolean feeBearing = chainAdapterRegistry
      .getByProtocol(currencyDto.getProtocol())
      .map(ChainAdapter::hasNetworkFee)
      // No adapter means the currency cannot be sent at all; that is reported
      // elsewhere, so this stays out of the way.
      .orElse(false);
    if (!feeBearing) {
      return null;
    }

    String feeEstimate = currencyDto.getFeeEstimate();
    boolean usable = false;
    if (feeEstimate != null && !feeEstimate.isBlank()) {
      try {
        usable = new BigInteger(feeEstimate.trim()).signum() > 0;
      } catch (NumberFormatException e) {
        usable = false;
      }
    }
    if (usable) {
      return null;
    }

    return (
      "### The current network fee for " +
      currencyDto.getName() +
      " (" +
      currencyDto.getTicker() +
      ") is not available right now, so withdrawals are paused.\n" +
      "-# This usually clears within a minute. Please try again shortly."
    );
  }

  /**
   * The wallet's own fee for this withdrawal, or unavailable when it cannot be
   * asked.
   *
   * <p>Goes through the entity rather than the DTO because the wallet
   * credentials are deliberately absent from the DTO. Any exception is treated
   * as unavailable: the quote improves the confirmation, it must never be the
   * reason a withdrawal cannot be made.
   */
  private FeeQuote quoteWithdrawalFee(
    CurrencyDto currencyDto,
    String raw,
    String address
  ) {
    try {
      Optional<CurrencyEntity> currencyEntity =
        currenciesService.getCurrencyByTicker(currencyDto.getTicker());
      if (currencyEntity.isEmpty()) {
        return FeeQuote.unavailable();
      }
      return chainAdapterRegistry
        .get(currencyEntity.get())
        .map(adapter ->
          adapter.quoteWithdrawalFee(currencyEntity.get(), raw, address)
        )
        .orElse(FeeQuote.unavailable());
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_SEND, e);
      return FeeQuote.unavailable();
    }
  }

  /**
   * Reverses a withdrawal debit whose queue entry could not be created, and
   * returns the message to show the user.
   *
   * <p>The debit and the queue insert are two separate writes, and nothing
   * downstream can repair a debit with no queue entry behind it: the chain
   * adapters only ever look at queued work, so the balance would simply be gone
   * with no record that anything was owed. Compensating immediately is the only
   * point at which enough context still exists to put it back.
   *
   * <p>Safe to do unconditionally here because the queue entry is what causes a
   * send to happen at all. No entry means nothing was, or ever will be,
   * broadcast.
   */
  private String returnDebitedFunds(String userId, WalletDto debited) {
    TransferResponseDto refundResponse = new TransferResponseDto();
    coreServices.commandsService.setCommands(
      Constants.COMMAND_NAME_RECEIVE,
      userId,
      refundResponse::setCommands
    );

    List<WalletDto> wallets = new ArrayList<>();
    wallets.add(new WalletDto(debited.getTicker(), debited.getRaw(), true));
    refundResponse.setPrimaryTransfer(
      new TransferDto(wallets, new ArrayList<>())
    );

    TransferResponseDto refund = transferExecutorService.executeTransfer(
      Constants.COMMAND_NAME_RECEIVE,
      null,
      null,
      "0",
      null,
      Collections.singletonList(userId),
      null,
      refundResponse
    );

    if (refund == null || refund.getTransactionId() == null) {
      LoggingUtil.errorLogging(
        Constants.COMMAND_NAME_SEND,
        new IllegalStateException(
          "Could not return " +
          debited.getRaw() +
          " " +
          debited.getTicker() +
          " to user " +
          userId +
          " after the withdrawal failed to queue; the balance is still debited " +
          "and needs manual correction."
        )
      );
      return (
        "### Something went wrong setting up your withdrawal, and your balance " +
        "could not be restored automatically.\n" +
        "Please contact support so it can be corrected:\n" +
        System.getenv("HOME_SERVER_INVITE_URL")
      );
    }

    return (
      "### Something went wrong setting up your withdrawal, so it was not sent.\n" +
      "-# Your funds have been returned to your balance. Please try again."
    );
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
          // Only an explicit false disables this. Unset means supported, which
          // keeps currency documents predating the field working and avoids
          // silently disabling /update for Nano and Banano.
          if (Boolean.FALSE.equals(currencyDto.getSupportsRepresentative())) {
            transferResponseDto.setErrorMessage(
              "### " +
              currencyDto.getName() +
              " (" +
              currencyDto.getTicker() +
              ") does not use representatives, so there is nothing to update."
            );
            return transferResponseDto;
          }
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
          // Enforced here the way /gift enforces minimumGift. On a network with
          // fees this is not just a policy floor: the fee is deducted from the
          // amount sent, so a withdrawal smaller than the fee cannot be built at
          // all, and the balance has already been debited by the time the send
          // is attempted.
          // Without an estimate the effective minimum silently collapses to the
          // policy floor alone, which on a fee-bearing network lets through a
          // withdrawal the fee will consume entirely. The balance is debited
          // before the send is attempted, so that costs the user a failed
          // withdrawal and a refund cycle rather than a clear rejection.
          String feeError = validateFeeEstimateAvailable(currencyDto);
          if (feeError != null) {
            transferResponseDto.setErrorMessage(feeError);
            return transferResponseDto;
          }

          String minimumError = currenciesService.validateMinimumAmount(
            currencyDto,
            transferResponseDto
              .getPrimaryTransfer()
              .getWallets()
              .get(0)
              .getRaw(),
            currenciesService.getEffectiveMinimumWithdraw(currencyDto),
            "Withdrawal"
          );
          if (minimumError != null) {
            transferResponseDto.setErrorMessage(minimumError);
            return transferResponseDto;
          }
          // Withdrawing to a Nanobot deposit address is allowed on purpose: it
          // is a useful self-test, and one user may legitimately withdraw to
          // another user's shared deposit address as an exchange. Chain adapters
          // are responsible for crediting the recipient of such a transfer.
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

                  // The balance is already gone by this point. If the queue
                  // entry does not survive, nothing will ever send it and
                  // nothing will ever refund it, so the debit has to be undone
                  // here rather than left for someone to find.
                  QueueEntity queueEntity = null;
                  try {
                    queueEntity = queuesService.createQueue(queueDto);
                  } catch (Exception e) {
                    LoggingUtil.errorLogging(Constants.COMMAND_NAME_SEND, e);
                  }
                  if (queueEntity == null) {
                    transferResponseDto.setErrorMessage(
                      returnDebitedFunds(
                        requestDto.getUserId(),
                        transferDto.getWallets().get(0)
                      )
                    );
                    return transferResponseDto;
                  }
                } else {
                  transferResponseDto.setErrorMessage(
                    transfer.getErrorMessage()
                  );
                  return transferResponseDto;
                }
              } else {
                // Preview pass. Ask the wallet what this exact withdrawal would
                // cost so the confirmation shows the real fee, and so a request
                // the wallet cannot build is refused here, before any debit,
                // instead of failing in the queue and being refunded.
                FeeQuote quote = quoteWithdrawalFee(
                  currencyDto,
                  transferResponseDto
                    .getPrimaryTransfer()
                    .getWallets()
                    .get(0)
                    .getRaw(),
                  requestDto.getAddress()
                );
                if (quote.status() == FeeQuote.Status.REJECTED) {
                  transferResponseDto.setErrorMessage(
                    quote.refusal().confirmationMessage()
                  );
                  return transferResponseDto;
                }
                if (quote.status() == FeeQuote.Status.DELAYED) {
                  transferResponseDto.setDelayed(true);
                  if (quote.refusal() != null) {
                    transferResponseDto.setDelayNotice(quote.refusal().reason());
                  }
                  transferResponseDto.setConfirmation(true);
                  return transferResponseDto;
                }
                if (quote.status() == FeeQuote.Status.QUOTED) {
                  transferResponseDto.setNetworkFee(quote.fee().toString());
                }
                transferResponseDto.setConfirmation(true);
                return transferResponseDto;
              }
            } else {
              // The regex is length-anchored per currency, so this also catches
              // an address of the wrong length for the requested ticker. Quote
              // the expected shape so the user can tell what went wrong.
              String expectedFormat = currencyDto.getAddressFormat();
              transferResponseDto.setErrorMessage(
                "Invalid Address: Please specify a valid " +
                currencyDto.getName() +
                " (" +
                currencyDto.getTicker() +
                ") address." +
                (expectedFormat == null || expectedFormat.isBlank()
                    ? ""
                    : "\n" +
                    currencyDto.getName() +
                    " addresses are " +
                    expectedFormat +
                    ".")
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
