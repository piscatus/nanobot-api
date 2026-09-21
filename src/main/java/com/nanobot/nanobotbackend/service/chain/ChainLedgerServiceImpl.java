package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.service.CoreServices;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.MessagesService;
import com.nanobot.nanobotbackend.service.TransactionsService;
import com.nanobot.nanobotbackend.service.TransferExecutorService;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChainLedgerServiceImpl implements ChainLedgerService {

  private static final String URL_PLACEHOLDER = "{value}";
  private static final String CONFIRMED_NOTE =
    "has been *confirmed* on the network!";
  private static final String DISCOVERED_NOTE =
    "has been *discovered* on the network!";
  private static final String SENT_NOTE = "has been *sent* to the network!";

  /**
   * Nano currencies predate the explorer templates on the currency document,
   * so a missing template falls back to the address the Nano path has always
   * linked to rather than rendering the notice without a link.
   */
  private static final String NANO_EXPLORER_BLOCK_URL =
    "https://nanexplorer.com/{name}/blocks/{value}";

  private final CoreServices coreServices;
  private final CurrenciesService currenciesService;
  private final MessagesService messagesService;
  private final TransactionsService transactionsService;
  private final FileLogger fileLogger;

  @Autowired
  private TransferExecutorService transferExecutorService;

  public ChainLedgerServiceImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    MessagesService messagesService,
    TransactionsService transactionsService
  ) {
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.messagesService = messagesService;
    this.transactionsService = transactionsService;
    this.fileLogger = new FileLogger("ChainLedgerService");
  }

  @Override
  public String creditDeposit(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    Map<String, String> commandMap
  ) {
    return creditDeposit(
      currencyEntity,
      userId,
      raw,
      txid,
      depositAddress,
      commandMap,
      true
    );
  }

  @Override
  public String creditDeposit(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    Map<String, String> commandMap,
    boolean notify
  ) {
    TransferResponseDto transferResponseDto = new TransferResponseDto();
    coreServices.commandsService.setCommands(
      Constants.COMMAND_NAME_RECEIVE,
      userId,
      transferResponseDto::setCommands
    );

    List<WalletDto> wallets = new ArrayList<>();
    wallets.add(new WalletDto(currencyEntity.getTicker(), raw, true));
    transferResponseDto.setPrimaryTransfer(
      new TransferDto(wallets, new ArrayList<>())
    );
    transferResponseDto.setBlockHash(txid);

    // "0" is the system account: crediting from it mints into the user's
    // balance, mirroring how the Nano path books a confirmed deposit.
    TransferResponseDto transfer = transferExecutorService.executeTransfer(
      Constants.COMMAND_NAME_RECEIVE,
      null,
      null,
      "0",
      null,
      Collections.singletonList(userId),
      null,
      transferResponseDto
    );

    if (transfer == null || transfer.getTransactionId() == null) {
      fileLogger.error(
        "Deposit transfer failed for user " +
        userId +
        " on " +
        currencyEntity.getTicker() +
        " tx " +
        txid
      );
      return null;
    }

    if (notify) {
      notifyDepositConfirmed(
        currencyEntity,
        userId,
        raw,
        txid,
        depositAddress,
        transfer.getTransactionId(),
        commandMap
      );
    }

    return transfer.getTransactionId();
  }

  @Override
  public void notifyDepositConfirmed(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    String transactionId,
    Map<String, String> commandMap
  ) {
    String decimalValue = currenciesService.getCurrencyDecimalValue(
      raw,
      Integer.parseInt(currencyEntity.getPrecision())
    );
    String footer = "Nanobot Transaction ID " + transactionId;
    String title = "🧾 Deposit Confirmed";

    String header =
      "<@" + userId + ">'s deposit " + CONFIRMED_NOTE + "\n";

    String commandNote =
      "\n-# Use </transactions:" +
      commandMap.get("transactions") +
      "> to view your transaction history." +
      "\n-# Use </wallet:" +
      commandMap.get("wallet") +
      "> to view your *updated* currency balances.\n";

    String body =
      "### 💼 __Wallet Credits__\n> **" +
      decimalValue +
      " " +
      currencyEntity.getTicker() +
      "** (≈$" +
      currenciesService.getCurrencyDollarValue(
        decimalValue,
        currencyEntity.getValue()
      ) +
      ") " +
      currencyEntity.getEmoji() +
      "\n### 🏠︎ __Deposit Address__\n> `" +
      depositAddress +
      "`\n### 🔗 __Transaction__\n> `" +
      txid +
      "`";

    String url = explorerTxUrl(currencyEntity, txid);

    messagesService.createMessage(
      new MessageDto(
        null,
        System.getenv("HOME_SERVER_ID"),
        System.getenv("DEPOSIT_LOGGING_CHANNEL_ID"),
        null,
        title,
        currencyEntity.getColor(),
        header + body,
        new Date(),
        url,
        null,
        footer
      )
    );

    messagesService.createMessage(
      new MessageDto(
        userId,
        null,
        null,
        null,
        title,
        currencyEntity.getColor(),
        header + commandNote + body,
        new Date(),
        url,
        null,
        footer
      )
    );
  }

  @Override
  public void notifyDepositDiscovered(
    CurrencyEntity currencyEntity,
    String userId,
    String raw,
    String txid,
    String depositAddress,
    long confirmations,
    int required,
    Map<String, String> commandMap
  ) {
    String decimalValue = currenciesService.getCurrencyDecimalValue(
      raw,
      Integer.parseInt(currencyEntity.getPrecision())
    );
    String title = "👀 Deposit Discovered";

    String header =
      "<@" + userId + ">'s deposit " + DISCOVERED_NOTE + "\n";

    // No /wallet pointer here: nothing has been credited yet, and sending
    // someone to check a balance that has not changed would only confuse them.
    String progressNote =
      "\n-# Funds will be credited after **" +
      pluralConfirmations(required) +
      "** (currently " +
      Math.max(0L, confirmations) +
      "). You will receive another message once confirmed.\n";

    String body =
      "### 💼 __Pending Credit__\n> **" +
      decimalValue +
      " " +
      currencyEntity.getTicker() +
      "** (≈$" +
      currenciesService.getCurrencyDollarValue(
        decimalValue,
        currencyEntity.getValue()
      ) +
      ") " +
      currencyEntity.getEmoji() +
      "\n### 🏠︎ __Deposit Address__\n> `" +
      depositAddress +
      "`\n### 🔗 __Transaction__\n> `" +
      txid +
      "`";

    String url = explorerTxUrl(currencyEntity, txid);

    messagesService.createMessage(
      new MessageDto(
        null,
        System.getenv("HOME_SERVER_ID"),
        System.getenv("DEPOSIT_LOGGING_CHANNEL_ID"),
        null,
        title,
        currencyEntity.getColor(),
        header + body,
        new Date(),
        url,
        null,
        null
      )
    );

    messagesService.createMessage(
      new MessageDto(
        userId,
        null,
        null,
        null,
        title,
        currencyEntity.getColor(),
        header + progressNote + body,
        new Date(),
        url,
        null,
        null
      )
    );
  }

  @Override
  public void notifyWithdrawalSent(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    int required,
    Map<String, String> commandMap,
    BigInteger networkFee
  ) {
    if (queueEntity.getUserId() == null) {
      return;
    }

    String decimalValue = currenciesService.getCurrencyDecimalValue(
      queueEntity.getRaw(),
      Integer.parseInt(currencyEntity.getPrecision())
    );

    String header =
      "<@" + queueEntity.getUserId() + ">'s withdrawal " + SENT_NOTE + "\n";

    String progressNote =
      "\n-# You will receive another message once it reaches **" +
      pluralConfirmations(required) +
      "**.\n";

    String body =
      "### 💸 __Currency Transfers__\n> **" +
      decimalValue +
      " " +
      currencyEntity.getTicker() +
      "** (≈$" +
      currenciesService.getCurrencyDollarValue(
        decimalValue,
        currencyEntity.getValue()
      ) +
      ") " +
      currencyEntity.getEmoji() +
      feeNote(currencyEntity, queueEntity, networkFee) +
      "\n### 🔗 __" +
      hashLabel(currencyEntity) +
      "__\n> `" +
      queueEntity.getBlockHash() +
      "`\n### 📌 __Withdrawal Address__\n> `" +
      queueEntity.getTargetAddress() +
      "`";

    String title = "📨 Withdrawal Sent";
    String url = explorerTxUrl(currencyEntity, queueEntity.getBlockHash());

    messagesService.createMessage(
      new MessageDto(
        null,
        System.getenv("HOME_SERVER_ID"),
        System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
        null,
        title,
        currencyEntity.getColor(),
        header + body,
        new Date(),
        url,
        null,
        null
      )
    );

    messagesService.createMessage(
      new MessageDto(
        queueEntity.getUserId(),
        null,
        null,
        null,
        title,
        currencyEntity.getColor(),
        header + progressNote + body,
        new Date(),
        url,
        null,
        null
      )
    );
  }

  @Override
  public void notifyWithdrawalDelayed(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    String reason,
    Map<String, String> commandMap
  ) {
    if (queueEntity.getUserId() == null) {
      return;
    }

    String decimalValue = currenciesService.getCurrencyDecimalValue(
      queueEntity.getRaw(),
      Integer.parseInt(currencyEntity.getPrecision())
    );

    String header =
      "<@" +
      queueEntity.getUserId() +
      ">'s withdrawal is *queued* and waiting to be sent.\n" +
      reason +
      "\n";

    String progressNote =
      "\n-# You will receive another message once it has been sent to the " +
      "network. No action is needed.\n";

    // No hash yet: nothing has been broadcast. The amount and address are
    // repeated so the message stands on its own in a DM history.
    String body =
      "### 💸 __Currency Transfers__\n> **" +
      decimalValue +
      " " +
      currencyEntity.getTicker() +
      "** (≈$" +
      currenciesService.getCurrencyDollarValue(
        decimalValue,
        currencyEntity.getValue()
      ) +
      ") " +
      currencyEntity.getEmoji() +
      "\n### 📌 __Withdrawal Address__\n> `" +
      queueEntity.getTargetAddress() +
      "`";

    String title = "⏳ Withdrawal Queued";

    messagesService.createMessage(
      new MessageDto(
        null,
        System.getenv("HOME_SERVER_ID"),
        System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
        null,
        title,
        currencyEntity.getColor(),
        header + body,
        new Date(),
        null,
        null,
        null
      )
    );

    messagesService.createMessage(
      new MessageDto(
        queueEntity.getUserId(),
        null,
        null,
        null,
        title,
        currencyEntity.getColor(),
        header + progressNote + body,
        new Date(),
        null,
        null,
        null
      )
    );
  }

  @Override
  public void notifyWithdrawalConfirmed(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    Map<String, String> commandMap,
    BigInteger networkFee
  ) {
    if (queueEntity.getUserId() == null) {
      return;
    }

    transactionsService.updateTransactionBlockHash(
      queueEntity.getTransactionId(),
      queueEntity.getBlockHash()
    );

    String decimalValue = currenciesService.getCurrencyDecimalValue(
      queueEntity.getRaw(),
      Integer.parseInt(currencyEntity.getPrecision())
    );

    String header =
      "<@" +
      queueEntity.getUserId() +
      ">'s withdrawal " +
      CONFIRMED_NOTE +
      "\n";

    String commandNote =
      "\n-# Use </transactions:" +
      commandMap.get("transactions") +
      "> to view your transaction history.\n";

    String body =
      "### 💸 __Currency Transfers__\n> **" +
      decimalValue +
      " " +
      currencyEntity.getTicker() +
      "** (≈$" +
      currenciesService.getCurrencyDollarValue(
        decimalValue,
        currencyEntity.getValue()
      ) +
      ") " +
      currencyEntity.getEmoji() +
      feeNote(currencyEntity, queueEntity, networkFee) +
      "\n### 🔗 __Transaction__\n> `" +
      queueEntity.getBlockHash() +
      "`\n### 📌 __Withdrawal Address__\n> `" +
      queueEntity.getTargetAddress() +
      "`";

    String title = "🧾 Withdrawal Confirmed";
    String url = explorerTxUrl(currencyEntity, queueEntity.getBlockHash());

    messagesService.createMessage(
      new MessageDto(
        null,
        System.getenv("HOME_SERVER_ID"),
        System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
        null,
        title,
        currencyEntity.getColor(),
        header + body,
        new Date(),
        url,
        null,
        null
      )
    );

    messagesService.createMessage(
      new MessageDto(
        queueEntity.getUserId(),
        null,
        null,
        null,
        title,
        currencyEntity.getColor(),
        header + commandNote + body,
        new Date(),
        url,
        null,
        null
      )
    );
  }

  @Override
  public String refundFailedWithdrawal(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    String reason,
    Map<String, String> commandMap
  ) {
    String userId = queueEntity.getUserId();
    if (userId == null) {
      return null;
    }

    TransferResponseDto transferResponseDto = new TransferResponseDto();
    coreServices.commandsService.setCommands(
      Constants.COMMAND_NAME_RECEIVE,
      userId,
      transferResponseDto::setCommands
    );

    List<WalletDto> wallets = new ArrayList<>();
    wallets.add(
      new WalletDto(currencyEntity.getTicker(), queueEntity.getRaw(), true)
    );
    transferResponseDto.setPrimaryTransfer(
      new TransferDto(wallets, new ArrayList<>())
    );

    // Mint back from the system account, the same path a deposit takes. The
    // balance was burned when the withdrawal was queued, so this restores it.
    TransferResponseDto transfer = transferExecutorService.executeTransfer(
      Constants.COMMAND_NAME_RECEIVE,
      null,
      null,
      "0",
      null,
      Collections.singletonList(userId),
      null,
      transferResponseDto
    );

    if (transfer == null || transfer.getTransactionId() == null) {
      fileLogger.error(
        "Refund transfer FAILED for user " +
        userId +
        " on " +
        currencyEntity.getTicker() +
        " amount " +
        queueEntity.getRaw() +
        "; balance remains debited and needs manual correction."
      );
      return null;
    }

    String decimalValue = currenciesService.getCurrencyDecimalValue(
      queueEntity.getRaw(),
      Integer.parseInt(currencyEntity.getPrecision())
    );

    String title = "💀 Withdrawal Failed";
    String footer = "Nanobot Transaction ID " + transfer.getTransactionId();

    String header =
      "<@" + userId + ">'s withdrawal *could not be completed*.\n" + reason + "\n";

    String commandNote =
      "\n-# Use </transactions:" +
      commandMap.get("transactions") +
      "> to view your transaction history." +
      "\n-# Use </wallet:" +
      commandMap.get("wallet") +
      "> to view your *updated* currency balances.\n";

    // Wallet Credits only. There is no block hash because nothing was
    // broadcast, and no deposit address because this is a refund rather than an
    // incoming deposit.
    String body =
      "### 💼 __Wallet Credits__\n> **" +
      decimalValue +
      " " +
      currencyEntity.getTicker() +
      "** (≈$" +
      currenciesService.getCurrencyDollarValue(
        decimalValue,
        currencyEntity.getValue()
      ) +
      ") " +
      currencyEntity.getEmoji();

    messagesService.createMessage(
      new MessageDto(
        null,
        System.getenv("HOME_SERVER_ID"),
        System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
        null,
        title,
        currencyEntity.getColor(),
        header + body,
        new Date(),
        null,
        null,
        footer
      )
    );

    messagesService.createMessage(
      new MessageDto(
        userId,
        null,
        null,
        null,
        title,
        currencyEntity.getColor(),
        header + commandNote + body,
        new Date(),
        null,
        null,
        footer
      )
    );

    fileLogger.warn(
      "Refunded failed " +
      currencyEntity.getTicker() +
      " withdrawal of " +
      queueEntity.getRaw() +
      " to user " +
      userId
    );

    return transfer.getTransactionId();
  }

  @Override
  public String explorerTxUrl(CurrencyEntity currencyEntity, String txid) {
    String url = applyTemplate(currencyEntity.getExplorerTxUrl(), txid);
    if (url == null && isNano(currencyEntity) && txid != null) {
      return NANO_EXPLORER_BLOCK_URL
        .replace("{name}", currencyEntity.getName().toLowerCase())
        .replace(URL_PLACEHOLDER, txid);
    }
    return url;
  }

  /** "1 confirmation" or "6 confirmations", for the notices that quote one. */
  static String pluralConfirmations(int required) {
    return required + (required == 1 ? " confirmation" : " confirmations");
  }

  /**
   * Nano is feeless, so the fee line the other chains carry would be a lie
   * there. When the broadcast fee is known, name it and what arrived; otherwise
   * keep the generic sentence so the notice still explains why the on-chain
   * amount is smaller than the debit.
   */
  private String feeNote(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    BigInteger networkFee
  ) {
    if (isNano(currencyEntity)) {
      return "";
    }
    if (networkFee != null && networkFee.signum() > 0) {
      int precision = Integer.parseInt(currencyEntity.getPrecision());
      String ticker = currencyEntity.getTicker();
      String formattedFee = currenciesService.getCurrencyDecimalValue(
        networkFee.toString(),
        precision
      );
      BigInteger received;
      try {
        received = new BigInteger(queueEntity.getRaw()).subtract(networkFee);
      } catch (NumberFormatException e) {
        received = BigInteger.ZERO;
      }
      if (received.signum() < 0) {
        received = BigInteger.ZERO;
      }
      String formattedReceived = currenciesService.getCurrencyDecimalValue(
        received.toString(),
        precision
      );
      return (
        "\n**The network fee was " +
        formattedFee +
        " " +
        ticker +
        ". The recipient received " +
        formattedReceived +
        " " +
        ticker +
        ".**"
      );
    }
    return "\n-# The network fee was deducted from the amount sent.";
  }

  /**
   * Nano has no transactions, only blocks, and the rest of the Nano messages
   * already say so; the notice keeps that wording so it reads as one voice.
   */
  private static String hashLabel(CurrencyEntity currencyEntity) {
    return isNano(currencyEntity) ? "Block Hash" : "Transaction";
  }

  private static boolean isNano(CurrencyEntity currencyEntity) {
    return Constants.PROTOCOL_NANO.equals(currencyEntity.getProtocol());
  }

  @Override
  public String explorerAccountUrl(
    CurrencyEntity currencyEntity,
    String address
  ) {
    return applyTemplate(currencyEntity.getExplorerAccountUrl(), address);
  }

  private String applyTemplate(String template, String value) {
    if (template == null || template.isBlank() || value == null) {
      return null;
    }
    return template.replace(URL_PLACEHOLDER, value);
  }
}
