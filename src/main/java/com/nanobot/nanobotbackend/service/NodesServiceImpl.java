package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.oczadly.karl.jnano.model.NanoAccount;

@Service
public class NodesServiceImpl implements NodesService {

  private static final int BATCH_SIZE = 100;

  private final FileLogger fileLogger;
  private final CoreServices coreServices;
  private final CurrenciesService currenciesService;
  private final MessagesService messagesService;
  private final QueuesService queuesService;
  private final TransactionsService transactionsService;
  private final HttpClient client;

  @Autowired
  private TransferExecutorService transferExecutorService;

  private final String botUserId = System.getenv("BOT_USER_ID");
  private final String workServerUrl;
  private static final String CONFIRMED_NOTE =
    "has been *confirmed* on the network!";
  private static final String BALANCE_RESPONSE_KEY = "balance";
  private static final String CONFIRMED_RESPONSE_KEY = "confirmed";
  private static final String BLOCK_COUNT_ACTION = "block_count";
  private static final String BLOCK_INFORMATION_ACTION = "block_info";
  private static final String ACTION_COMMAND = "action";

  @Autowired
  public NodesServiceImpl(
    CurrenciesService currenciesService,
    MessagesService messagesService,
    QueuesService queuesService,
    CoreServices coreServices,
    TransactionsService transactionsService
  ) {
    this(
      currenciesService,
      messagesService,
      queuesService,
      coreServices,
      transactionsService,
      HttpClient.newHttpClient(),
      System.getenv("WORK_SERVER_URL")
    );
  }

  /**
   * Constructor for testing: allows injecting HttpClient and work server URL
   * so that MockWebServer or other test doubles can be used.
   */
  NodesServiceImpl(
    CurrenciesService currenciesService,
    MessagesService messagesService,
    QueuesService queuesService,
    CoreServices coreServices,
    TransactionsService transactionsService,
    HttpClient client,
    String workServerUrl
  ) {
    this.fileLogger = new FileLogger("NodesService");
    this.currenciesService = currenciesService;
    this.messagesService = messagesService;
    this.coreServices = coreServices;
    this.transactionsService = transactionsService;
    this.queuesService = queuesService;
    this.client = client;
    this.workServerUrl = workServerUrl;
  }

  public static int getCharsBeforeUnderscore(String input) {
    // Find position of underscore
    int underscoreIndex = input.indexOf('_');
    return (underscoreIndex >= 0) ? underscoreIndex : 0;
  }

  public static byte[] addressToPublicKey(
    CurrencyEntity currencyEntity,
    String address
  ) {
    String raw = address.substring(getCharsBeforeUnderscore(address) + 1);

    // Remove checksum (last 8 chars)
    String withoutChecksum = raw.substring(0, raw.length() - 8);

    // Base32 decode (Banano uses a custom Base32 alphabet)
    String alphabet = "13456789abcdefghijkmnopqrstuwxyz";
    BigInteger result = BigInteger.ZERO;
    for (char c : withoutChecksum.toCharArray()) {
      int val = alphabet.indexOf(c);
      if (val == -1) throw new IllegalArgumentException(
        "Invalid char in address"
      );
      result = result.shiftLeft(5).add(BigInteger.valueOf(val));
    }

    // Convert to 32-byte array
    byte[] bytes = result.toByteArray();

    // BigInteger may add a leading 0 for sign, ensure exactly 32 bytes
    byte[] publicKey = new byte[32];
    int copyStart = Math.max(0, bytes.length - 32);
    int copyLength = Math.min(32, bytes.length);
    System.arraycopy(bytes, copyStart, publicKey, 32 - copyLength, copyLength);

    return publicKey;
  }

  public JSONObject getWorkBody(String hash, String difficulty) {
    JSONObject requestWorkBody = new JSONObject()
      .put(ACTION_COMMAND, "work_generate")
      .put("hash", hash)
      .put("difficulty", difficulty);

    return requestWorkBody;
  }

  public void processActivity(
    UserDetailsEntity botUserDetails,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    String botUserSeed = botUserDetails.getSeed();
    String currencyTicker = currencyEntity.getTicker();
    String nodeUrl = currencyEntity.getNodeUrl();
    if (
      !sendRequestToNode(
        null,
        null,
        null,
        currencyEntity,
        BLOCK_COUNT_ACTION,
        new JSONObject().put(ACTION_COMMAND, BLOCK_COUNT_ACTION)
      )
    ) {
      fileLogger.warn(currencyTicker + " node is OFFLINE!");
      return;
    }

    //this is the consolidation account, the hot wallet
    String botUserAddress = CryptoUtil.deriveAddressFromSeed(
      botUserSeed,
      currencyTicker
    );

    if (currencyEntity.getProcessWithdrawals()) {
      processWithdrawals(currencyEntity, commandMap);
    } else {
      fileLogger.warn(
        "Not currently processing withdrawals for " + currencyTicker + "!"
      );
    }
    if (currencyEntity.getProcessDeposits()) {
      processDeposits(currencyEntity, botUserAddress, botUserSeed, commandMap);
    } else {
      fileLogger.warn(
        "Not currently processing deposits for " + currencyTicker + "!"
      );
    }
  }

  public void processSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity
  ) {
    // get the account info
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "account_info")
        .put("representative", true)
        .put("account", queueEntity.getSourceAddress())
    );

    if (
      response == null ||
      (response.has("error") &&
        "Account not found".equals(response.getString("error")))
    ) {
      fileLogger.error(
        "Failed to get account info for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node."
      );
      return;
    }

    // generate the work
    JSONObject workResponse = sendRequestToWorkServer(
      workServerUrl,
      getWorkBody(
        response.getString("frontier"),
        currencyEntity.getSendDifficulty()
      )
    );

    // get the private key from the seed
    byte[] seedBytes = hexStringToByteArray(queueEntity.getSeed());
    byte[] privateKey = derivePrivateKey(seedBytes, 0);

    // send create block request
    JSONObject createBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "block_create")
        .put("json_block", "true")
        .put("type", "state")
        .put(
          BALANCE_RESPONSE_KEY,
          new BigInteger(response.getString(BALANCE_RESPONSE_KEY))
            .subtract(new BigInteger(queueEntity.getRaw()))
            .toString()
        )
        .put("key", bytesToHex(privateKey))
        .put("representative", response.getString("representative"))
        .put(
          "link",
          bytesToHex(
            addressToPublicKey(currencyEntity, queueEntity.getTargetAddress())
          )
        )
        .put("previous", response.getString("frontier"))
        .put("work", workResponse.getString("work"))
    );

    if (createBlockResponse == null || (createBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to create block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        createBlockResponse.toString()
      );
      return;
    }

    JSONObject blockObject = createBlockResponse.getJSONObject("block");

    // send process block request
    JSONObject processBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "process")
        .put("json_block", "true")
        .put("block", blockObject)
    );

    if (processBlockResponse == null || (processBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to process send block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        processBlockResponse.getString("error")
      );
      return;
    }

    // if successful update to processed and add xid
    queueEntity.setBlockHash(processBlockResponse.getString("hash"));
    queueEntity.setProcessed(true);
    queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));

    fileLogger.info(
      "Successfully processed send transaction: " + queueEntity.getBlockHash()
    );
  }

  public void confirmSend(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    // get the transaction details
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
        .put("hash", queueEntity.getBlockHash())
    );
    if (!Boolean.parseBoolean(response.getString(CONFIRMED_RESPONSE_KEY))) {
      return;
    }
    Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deletedQueue.isPresent()) {
      fileLogger.info(
        "Successfully removed confirmed send - queue #" + queueEntity.getId()
      );

      if (
        queueEntity.getUserId() == null ||
        queueEntity.getUserId().equals(botUserId)
      ) {
        return;
      }

      transactionsService.updateTransactionBlockHash(
        queueEntity.getTransactionId(),
        queueEntity.getBlockHash()
      );

      String decimalValue = currenciesService.getCurrencyDecimalValue(
        queueEntity.getRaw(),
        Integer.valueOf(currencyEntity.getPrecision())
      );

      String messageDescriptionTitle =
        "<@" +
        queueEntity.getUserId() +
        ">'s withdrawal " +
        CONFIRMED_NOTE +
        "\n";

      String commandNote =
        "\n-# Use </transactions:" +
        commandMap.get("transactions") +
        "> to view your transaction history." +
        "\n";

      String messageDescriptionContent =
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
        "\n" +
        "### 🔗 __Block Hash__\n> `" +
        queueEntity.getBlockHash() +
        "`" +
        "\n" +
        "### 📌 __Withdrawal Address__\n> `" +
        queueEntity.getTargetAddress() +
        "`";

      String userMessageDescription =
        messageDescriptionTitle + commandNote + messageDescriptionContent;

      String systemMessageDescription =
        messageDescriptionTitle + messageDescriptionContent;

      messagesService.createMessage(
        new MessageDto(
          null,
          System.getenv("HOME_SERVER_ID"),
          System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
          null,
          "🧾 Withdrawal Confirmed",
          currencyEntity.getColor(),
          systemMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
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
          "🧾 Withdrawal Confirmed",
          currencyEntity.getColor(),
          userMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          null
        )
      );
    }
  }

  public void processUpdate(
    CurrencyEntity currencyEntity,
    QueueEntity queueEntity,
    Map<String, String> commandMap
  ) {
    // get the account info
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "account_info")
        .put("representative", true)
        .put("account", queueEntity.getSourceAddress())
    );

    if (
      response == null ||
      (response.has("error") &&
        "Account not found".equals(response.getString("error")))
    ) {
      Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
        queueEntity.getId()
      );
      if (deletedQueue.isPresent()) {
        fileLogger.info(
          "Successfully removed confirmed update - queue #" +
          queueEntity.getId()
        );

        String depositAddressDescription =
          "### 🏠︎ __Deposit Address__\n> `" +
          queueEntity.getSourceAddress() +
          "`";

        String userMessageDescription =
          "<@" +
          queueEntity.getUserId() +
          ">'s update request *unsuccessfully* completed.\n" +
          "## Your representative update to `" +
          queueEntity.getTargetAddress() +
          "` failed to process because you have not sent " +
          currencyEntity.getName() +
          " to your **" +
          queueEntity.getTicker() +
          "** " +
          currencyEntity.getEmoji() +
          " deposit address.\n" +
          "### This is a one time process, and can be achieved by using </" +
          Constants.COMMAND_NAME_SEND +
          ":" +
          commandMap.get("send") +
          "> to withdraw as little as " +
          currenciesService.getCurrencyDecimalValue(
            currencyEntity.getMinimumWithdraw(),
            Integer.valueOf(currencyEntity.getPrecision())
          ) +
          " " +
          currencyEntity.getTicker() +
          " (" +
          currencyEntity.getPrecision() +
          " decimal places) to your deposit address." +
          "\n" +
          depositAddressDescription;

        String systemMessageDescription =
          "<@" +
          queueEntity.getUserId() +
          ">'s update request *unsuccessfully* completed due to their deposit account being unopened." +
          "\n" +
          depositAddressDescription;

        messagesService.createMessage(
          new MessageDto(
            null,
            System.getenv("HOME_SERVER_ID"),
            System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
            null,
            "💀 Update Failed",
            currencyEntity.getColor(),
            systemMessageDescription,
            new java.util.Date(),
            "https://nanexplorer.com/" +
            currencyEntity.getName().toLowerCase() +
            "/accounts/" +
            queueEntity.getSourceAddress(),
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
            "💀 Update Failed",
            currencyEntity.getColor(),
            userMessageDescription,
            new java.util.Date(),
            "https://nanexplorer.com/" +
            currencyEntity.getName().toLowerCase() +
            "/accounts/" +
            queueEntity.getSourceAddress(),
            null,
            null
          )
        );

        return;
      } else {
        return;
      }
    }

    // generate the work
    JSONObject workResponse = sendRequestToWorkServer(
      workServerUrl,
      getWorkBody(
        response.getString("frontier"),
        currencyEntity.getSendDifficulty()
      )
    );

    // get the private key from the seed
    byte[] seedBytes = hexStringToByteArray(queueEntity.getSeed());
    byte[] privateKey = derivePrivateKey(seedBytes, 0);

    // send create block request
    JSONObject createBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "block_create")
        .put("json_block", "true")
        .put("type", "state")
        .put(BALANCE_RESPONSE_KEY, response.getString(BALANCE_RESPONSE_KEY))
        .put("key", bytesToHex(privateKey))
        .put("representative", queueEntity.getTargetAddress())
        .put(
          "link",
          "0000000000000000000000000000000000000000000000000000000000000000"
        )
        .put("previous", response.getString("frontier"))
        .put("work", workResponse.getString("work"))
    );

    if (createBlockResponse == null || (createBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to create block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node."
      );
      return;
    }

    JSONObject blockObject = createBlockResponse.getJSONObject("block");

    // send process block request
    JSONObject processBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "process")
        .put("json_block", "true")
        .put("block", blockObject)
    );

    if (processBlockResponse == null || (processBlockResponse.has("error"))) {
      fileLogger.error(
        "Failed to process update block for " +
        queueEntity.getSourceAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        processBlockResponse.getString("error")
      );
      return;
    }

    // if successful update to processed and add xid
    queueEntity.setBlockHash(processBlockResponse.getString("hash"));
    queueEntity.setProcessed(true);
    queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));

    fileLogger.info(
      "Successfully processed update block: " + queueEntity.getBlockHash()
    );
  }

  public void confirmUpdate(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
        .put("hash", queueEntity.getBlockHash())
    );

    if (!Boolean.parseBoolean(response.getString(CONFIRMED_RESPONSE_KEY))) {
      return;
    }
    Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deletedQueue.isPresent()) {
      fileLogger.info(
        "Successfully removed confirmed update - queue #" + queueEntity.getId()
      );

      String messageDescription =
        "<@" +
        queueEntity.getUserId() +
        ">'s " +
        queueEntity.getTicker() +
        " " +
        currencyEntity.getEmoji() +
        " update " +
        CONFIRMED_NOTE +
        "\n" +
        "### 📌 __Representative Address__\n> `" +
        queueEntity.getTargetAddress() +
        "`" +
        "\n" +
        "### 🔗 __Block Hash__\n> `" +
        queueEntity.getBlockHash() +
        "`";

      messagesService.createMessage(
        new MessageDto(
          null,
          System.getenv("HOME_SERVER_ID"),
          System.getenv("WITHDRAWAL_LOGGING_CHANNEL_ID"),
          null,
          "🧾 Update Confirmed",
          currencyEntity.getColor(),
          messageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
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
          "🧾 Update Confirmed",
          currencyEntity.getColor(),
          messageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          null
        )
      );
    }
  }

  public void processReceive(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity
  ) {
    // 1. Get account info
    JSONObject response = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "account_info")
        .put("representative", true)
        .put("account", queueEntity.getTargetAddress())
    );

    fileLogger.info(
      "Entity ID: " +
      queueEntity.getId() +
      " Process Block Response: " +
      response.toString()
    );

    BigInteger currentBalance = BigInteger.ZERO;
    String previous;
    String workHash;
    String difficulty;

    // 2. Derive private key
    byte[] seedBytes = hexStringToByteArray(queueEntity.getSeed());
    byte[] privateKey = derivePrivateKey(seedBytes, 0);

    // 3. Determine if account exists
    if (
      response == null ||
      (response.has("error") &&
        "Account not found".equals(response.getString("error")))
    ) {
      fileLogger.info(
        "Account " +
        queueEntity.getTargetAddress() +
        " does not exist yet. This will be the first receive block."
      );
      previous =
        "0000000000000000000000000000000000000000000000000000000000000000";
      workHash = bytesToHex(
        addressToPublicKey(currencyEntity, queueEntity.getTargetAddress())
      );
      difficulty = currencyEntity.getOpenDifficulty();
    } else {
      currentBalance = new BigInteger(response.getString(BALANCE_RESPONSE_KEY));
      previous = response.getString("frontier");
      workHash = previous; // generate work on frontier
      difficulty = currencyEntity.getReceiveDifficulty();
    }

    JSONObject workResponse = sendRequestToWorkServer(
      workServerUrl,
      getWorkBody(workHash, difficulty)
    );

    // 5. Determine new balance
    BigInteger newBalance = currentBalance.add(
      new BigInteger(queueEntity.getRaw())
    );

    // 6. Create the receive block
    JSONObject createBlockRequest = new JSONObject()
      .put(ACTION_COMMAND, "block_create")
      .put("json_block", "true")
      .put("type", "state")
      .put(BALANCE_RESPONSE_KEY, newBalance.toString())
      .put("key", bytesToHex(privateKey))
      .put(
        "representative",
        response.has("representative")
          ? response.getString("representative")
          : CryptoUtil.deriveAddressFromSeed(
            queueEntity.getSeed(),
            queueEntity.getTicker()
          )
      )
      .put("link", queueEntity.getBlockHash()) // pending send block
      .put("previous", previous) // always include previous
      .put("work", workResponse.getString("work"));

    JSONObject createBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      createBlockRequest
    );

    if (createBlockResponse == null || createBlockResponse.has("error")) {
      fileLogger.error(
        "Failed to create receive block for " +
        queueEntity.getTargetAddress() +
        " on " +
        queueEntity.getTicker() +
        " node."
      );
      return;
    }

    JSONObject blockObject = createBlockResponse.getJSONObject("block");

    JSONObject processBlockResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, "process")
        .put("json_block", "true")
        .put("block", blockObject)
    );

    if (processBlockResponse == null || processBlockResponse.has("error")) {
      fileLogger.error(
        "Failed to process receive block for " +
        queueEntity.getTargetAddress() +
        " on " +
        queueEntity.getTicker() +
        " node: " +
        processBlockResponse.getString("error")
      );
      return;
    }

    queueEntity.setProcessed(true);
    queueEntity.setBlockHash(processBlockResponse.getString("hash"));
    queuesService.updateQueue(queueEntity.getId(), new QueueDto(queueEntity));

    fileLogger.info(
      "Successfully processed receive block: " + queueEntity.getBlockHash()
    );
  }

  public void confirmReceive(
    QueueEntity queueEntity,
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    JSONObject confirmResponse = getResponseFromNode(
      currencyEntity.getNodeUrl(),
      new JSONObject()
        .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
        .put("hash", queueEntity.getBlockHash())
    );

    if (
      !Boolean.parseBoolean(confirmResponse.getString(CONFIRMED_RESPONSE_KEY))
    ) {
      return;
    }
    Optional<QueueEntity> deletedQueue = queuesService.deleteQueue(
      queueEntity.getId()
    );
    if (deletedQueue.isPresent()) {
      fileLogger.info(
        "Successfully removed confirmed receive - queue #" + queueEntity.getId()
      );

      if (
        queueEntity.getUserId() == null ||
        queueEntity.getUserId().equals(botUserId)
      ) {
        return;
      }

      // credit the user
      TransferResponseDto transferResponseDto = new TransferResponseDto();
      coreServices.commandsService.setCommands(
        Constants.COMMAND_NAME_RECEIVE,
        queueEntity.getUserId(),
        transferResponseDto::setCommands
      );

      WalletDto walletDto = new WalletDto(
        queueEntity.getTicker(),
        queueEntity.getRaw(),
        true
      );
      List<WalletDto> wallets = new ArrayList<>();
      wallets.add(walletDto);
      TransferDto transferDto = new TransferDto(wallets, new ArrayList<>());
      transferResponseDto.setPrimaryTransfer(transferDto);

      transferResponseDto.setBlockHash(queueEntity.getBlockHash());

      TransferResponseDto transfer = transferExecutorService.executeTransfer(
        Constants.COMMAND_NAME_RECEIVE,
        null,
        null,
        "0",
        null,
        Collections.singletonList(queueEntity.getUserId()),
        null,
        transferResponseDto
      );

      String footerContent =
        "Nanobot Transaction ID " + transfer.getTransactionId();

      String depositConfirmedTitle = "🧾 Deposit Confirmed";

      String decimalValue = currenciesService.getCurrencyDecimalValue(
        queueEntity.getRaw(),
        Integer.valueOf(currencyEntity.getPrecision())
      );

      String messageDescriptionTitle =
        "<@" + queueEntity.getUserId() + ">'s deposit " + CONFIRMED_NOTE + "\n";

      String commandNote =
        "\n-# Use </transactions:" +
        commandMap.get("transactions") +
        "> to view your transaction history." +
        "\n" +
        "-# Use </wallet:" +
        commandMap.get("wallet") +
        "> to view your *updated* currency balances." +
        "\n";

      String messageDescriptionContent =
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
        "\n" +
        "### 🏠︎ __Deposit Address__\n> `" +
        queueEntity.getTargetAddress() +
        "`" +
        "\n" +
        "### 🔗 __Block Hash__\n> `" +
        queueEntity.getBlockHash() +
        "`";

      String userMessageDescription =
        messageDescriptionTitle + commandNote + messageDescriptionContent;

      String systemMessageDescription =
        messageDescriptionTitle + messageDescriptionContent;

      messagesService.createMessage(
        new MessageDto(
          null,
          System.getenv("HOME_SERVER_ID"),
          System.getenv("DEPOSIT_LOGGING_CHANNEL_ID"),
          null,
          depositConfirmedTitle,
          currencyEntity.getColor(),
          systemMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          footerContent
        )
      );

      messagesService.createMessage(
        new MessageDto(
          queueEntity.getUserId(),
          null,
          null,
          null,
          depositConfirmedTitle,
          currencyEntity.getColor(),
          userMessageDescription,
          new java.util.Date(),
          "https://nanexplorer.com/" +
          currencyEntity.getName().toLowerCase() +
          "/blocks/" +
          queueEntity.getBlockHash(),
          null,
          footerContent
        )
      );
    }
  }

  public void processWithdrawals(
    CurrencyEntity currencyEntity,
    Map<String, String> commandMap
  ) {
    String currencyTicker = currencyEntity.getTicker();
    List<QueueEntity> allQueues = queuesService.getQueues();
    for (QueueEntity queueEntity : allQueues) {
      if (queueEntity.getTicker().equals(currencyTicker)) {
        if (
          queueEntity.getLevel() == LevelDto.SEND && !queueEntity.getProcessed()
        ) {
          processSend(queueEntity, currencyEntity);
        } else if (
          queueEntity.getLevel() == LevelDto.SEND && queueEntity.getProcessed()
        ) {
          confirmSend(queueEntity, currencyEntity, commandMap);
        } else if (
          queueEntity.getLevel() == LevelDto.UPDATE &&
          !queueEntity.getProcessed()
        ) {
          processUpdate(currencyEntity, queueEntity, commandMap);
        } else if (
          queueEntity.getLevel() == LevelDto.UPDATE &&
          queueEntity.getProcessed()
        ) {
          confirmUpdate(queueEntity, currencyEntity, commandMap);
        }
      }
    }
  }

  public void processDeposits(
    CurrencyEntity currencyEntity,
    String botUserAddress,
    String botUserSeed,
    Map<String, String> commandMap
  ) {
    List<UserDetailsEntity> allUserDetails =
      coreServices.userDetailsService.getUsersDetails(null);
    Map<String, UserDetailsEntity> addressMap = new HashMap<>();

    for (UserDetailsEntity userDetails : allUserDetails) {
      if (userDetails.getSeed() == null) {
        continue;
      }

      String address = CryptoUtil.deriveAddressFromSeed(
        userDetails.getSeed(),
        currencyEntity.getTicker()
      );
      addressMap.put(address, userDetails);

      if (addressMap.size() >= BATCH_SIZE) {
        processAddressBatch(
          addressMap,
          currencyEntity,
          botUserAddress,
          botUserSeed
        );
        addressMap.clear();
      }
    }

    if (!addressMap.isEmpty()) {
      processAddressBatch(
        addressMap,
        currencyEntity,
        botUserAddress,
        botUserSeed
      );
    }

    List<QueueEntity> allQueues = queuesService.getQueues();
    for (QueueEntity queueEntity : allQueues) {
      if (queueEntity.getTicker().equals(currencyEntity.getTicker())) {
        if (
          queueEntity.getLevel() == LevelDto.RECEIVE &&
          !queueEntity.getProcessed()
        ) {
          processReceive(queueEntity, currencyEntity);
        } else if (
          queueEntity.getLevel() == LevelDto.RECEIVE &&
          queueEntity.getProcessed()
        ) {
          confirmReceive(queueEntity, currencyEntity, commandMap);
        }
      }
    }
  }

  @Override
  public void checkActivity() {
    // get the bot user details
    Optional<UserDetailsEntity> userDetailsOptional =
      coreServices.userDetailsService.getUserDetailsByUserId(botUserId);
    if (!userDetailsOptional.isPresent()) {
      fileLogger.error(
        "Bot user details not found, cannot proceed with node checks."
      );
      return;
    }
    UserDetailsEntity botUserDetails = userDetailsOptional.get();

    List<CommandEntity> entityCommands =
      coreServices.commandsService.getCommands(null);

    List<CommandDto> commands = new ArrayList<>();
    for (CommandEntity commandEntity : entityCommands) {
      commands.add(new CommandDto(commandEntity));
    }

    Map<String, String> commandMap = commands
      .stream()
      .collect(Collectors.toMap(CommandDto::getName, CommandDto::getCommandId));

    List<CurrencyEntity> currencies = currenciesService.getCurrencies(null);
    for (CurrencyEntity currencyEntity : currencies) {
      processActivity(botUserDetails, currencyEntity, commandMap);
    }
  }

  public void processAddressBatch(
    Map<String, UserDetailsEntity> addressMap,
    CurrencyEntity currency,
    String botAddress,
    String botUserSeed
  ) {
    List<String> newAddressBatch = new ArrayList<>(addressMap.keySet());
    String accountsBalancesAction = "accounts_balances";
    String accountsReceivableAction = "accounts_receivable";
    sendRequestToNode(
      addressMap,
      botAddress,
      botUserSeed,
      currency,
      accountsBalancesAction,
      new JSONObject()
        .put(ACTION_COMMAND, accountsBalancesAction)
        .put("accounts", new JSONArray(newAddressBatch))
    );
    sendRequestToNode(
      addressMap,
      botAddress,
      botUserSeed,
      currency,
      accountsReceivableAction,
      new JSONObject()
        .put(ACTION_COMMAND, accountsReceivableAction)
        .put("accounts", new JSONArray(newAddressBatch))
    );
  }

  public JSONObject sendRequestToWorkServer(
    String workServerUrl,
    JSONObject requestBody
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(workServerUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build();

      CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
      );

      HttpResponse<String> httpWorkResponse = responseFuture.join();

      return new JSONObject(httpWorkResponse.body());
    } catch (Exception e) {
      fileLogger.error(
        "Error executing work server request: " + e.getMessage()
      );
      return null;
    }
  }

  public boolean sendRequestToNode(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    String botUserSeed,
    CurrencyEntity currency,
    String action,
    JSONObject requestBody
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(currency.getNodeUrl()))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build();

      CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
      );
      HttpResponse<String> response = responseFuture.get();
      return handleResponse(
        addressMap,
        botAddress,
        botUserSeed,
        currency,
        action,
        response
      );
    } catch (JSONException e) {
      fileLogger.error("Error creating JSON request: " + e.getMessage());
      return false;
    } catch (InterruptedException | ExecutionException e) {
      fileLogger.error("Error executing node request: " + e.getMessage());
      return false;
    }
  }

  public JSONObject getResponseFromNode(
    String nodeUrl,
    JSONObject requestBody
  ) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(nodeUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
        .build();

      CompletableFuture<HttpResponse<String>> responseFuture = client.sendAsync(
        request,
        HttpResponse.BodyHandlers.ofString()
      );
      HttpResponse<String> response = responseFuture.get();
      if (response.statusCode() == 200) {
        return new JSONObject(response.body());
      } else {
        fileLogger.error(
          "Failed to get response from " +
          nodeUrl +
          " node. Status code: " +
          response.statusCode()
        );
        return null;
      }
    } catch (InterruptedException | ExecutionException e) {
      fileLogger.error("Error executing node request: " + e.getMessage());
      return null;
    }
  }

  public boolean handleResponse(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    String botUserSeed,
    CurrencyEntity currency,
    String action,
    HttpResponse<String> response
  ) {
    if (response.statusCode() == 200) {
      String responseData = response.body();
      try {
        JSONObject responseJson = new JSONObject(responseData);
        if ("accounts_balances".equals(action)) {
          processBalances(addressMap, botAddress, currency, responseJson);
          return true;
        } else if ("accounts_receivable".equals(action)) {
          processReceivable(
            addressMap,
            botAddress,
            botUserSeed,
            currency.getTicker(),
            currency.getNodeUrl(),
            responseJson
          );
          return true;
        } else if (BLOCK_COUNT_ACTION.equals(action)) {
          return processBlockCount(responseJson);
        }
      } catch (JSONException e) {
        fileLogger.error(
          "Failed to parse response JSON for " + action + ": " + e.getMessage()
        );
        return false;
      }
    } else {
      fileLogger.error(
        "Failed to get response from " +
        currency.getNodeUrl() +
        " node. Status code: " +
        response.statusCode()
      );
    }
    return false;
  }

  public boolean processBlockCount(JSONObject responseJson) {
    try {
      int uncheckedBlocks = responseJson.getInt("unchecked");
      int cementedBlocks = responseJson.getInt("cemented");
      int totalCount = responseJson.getInt("count");

      // 1. Check for hard cementation stalls (like your current Banano node issue)
      // If the node knows about blocks but is lagging behind by a severe margin (e.g., > 50 blocks)
      int synchronizationLag = totalCount - cementedBlocks;
      if (synchronizationLag > 50) {
        fileLogger.warn(
          "Node is falling behind. Uncemented block lag: " + synchronizationLag
        );
        return false;
      }

      // 2. Node is at the tip of its known ledger
      // (Note: To catch the "isolated node" edge case completely, you'll eventually want
      // to compare totalCount against an external telemetry avg or public explorer RPC)
      if (totalCount <= cementedBlocks) {
        // Node has cemented everything it currently knows about
        return true;
      }

      // Catch-all: Node is actively catching up a tiny, acceptable gap
      return true;
    } catch (JSONException e) {
      fileLogger.error("Failed to parse block count JSON: " + e.getMessage());
      return false;
    }
  }

  public static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) +
        Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static byte[] derivePrivateKey(byte[] seed, int index) {
    byte[] indexBytes = ByteBuffer.allocate(4).putInt(index).array(); // big-endian
    byte[] data = new byte[seed.length + 4];
    System.arraycopy(seed, 0, data, 0, seed.length);
    System.arraycopy(indexBytes, 0, data, seed.length, 4);
    Blake2bDigest digest = new Blake2bDigest(256); // 256-bit output
    digest.update(data, 0, data.length);
    byte[] privateKey = new byte[32];
    digest.doFinal(privateKey, 0);
    return privateKey;
  }

  public void processBalances(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    CurrencyEntity currencyEntity,
    JSONObject responseJson
  ) {
    try {
      JSONObject balances = responseJson.getJSONObject("balances");
      for (String account : balances.keySet()) {
        JSONObject accountBalance = balances.getJSONObject(account);
        String balance = accountBalance.getString(BALANCE_RESPONSE_KEY);
        if (botAddress.equals(account)) {
          if (!balance.equals(currencyEntity.getLiquidity())) {
            currencyEntity.setLiquidity(balance);
            currenciesService.updateCurrency(
              currencyEntity.getId(),
              new CurrencyDto(currencyEntity)
            );
          }
          continue;
        }
        if (!balance.equals("0")) {
          boolean isQueued = false;

          List<QueueEntity> existingQueues = queuesService.getQueues();

          for (QueueEntity queue : existingQueues) {
            if (
              queue.getSourceAddress() != null &&
              queue.getSourceAddress().equals(account) &&
              queue.getTicker().equals(currencyEntity.getTicker()) &&
              queue.getLevel() == LevelDto.SEND
            ) {
              isQueued = true;
              break;
            }
          }

          if (!isQueued) {
            fileLogger.info(
              "Adding account " +
              account +
              " with balance " +
              balance +
              " to the queue for ticker " +
              currencyEntity.getTicker()
            );

            QueueDto newQueue = new QueueDto(
              null,
              account,
              botAddress,
              LevelDto.SEND,
              null,
              balance,
              currencyEntity.getTicker(),
              false,
              addressMap.get(account).getSeed(),
              new java.util.Date(),
              null
            );

            queuesService.createQueue(newQueue);

            fileLogger.info(
              "Added account " +
              account +
              " with balance " +
              balance +
              " to the queue for ticker " +
              currencyEntity.getTicker()
            );
          }
        }
      }
    } catch (JSONException e) {
      fileLogger.error("Failed to parse balances JSON: " + e.getMessage());
    }
  }

  public void processReceivable(
    Map<String, UserDetailsEntity> addressMap,
    String botAddress,
    String botUserSeed,
    String ticker,
    String nodeUrl,
    JSONObject responseJson
  ) {
    try {
      JSONObject blocks = responseJson.getJSONObject("blocks");
      for (String account : blocks.keySet()) {
        JSONArray accountHashes = blocks.getJSONArray(account);
        for (int i = 0; i < accountHashes.length(); i++) {
          String hash = accountHashes.getString(i);
          boolean isQueued = false;
          List<QueueEntity> existingQueues = queuesService.getQueues();
          for (QueueEntity queue : existingQueues) {
            if (
              queue.getBlockHash() != null &&
              queue.getBlockHash().equals(hash) &&
              queue.getTicker().equals(ticker)
            ) {
              isQueued = true;
              break;
            }
          }

          if (isQueued) {
            continue;
          }

          JSONObject response = getResponseFromNode(
            nodeUrl,
            new JSONObject()
              .put(ACTION_COMMAND, BLOCK_INFORMATION_ACTION)
              .put("hash", hash)
          );

          if (
            !Boolean.parseBoolean(response.getString(CONFIRMED_RESPONSE_KEY))
          ) {
            continue;
          }

          fileLogger.info(
            "Receivable transaction has been confirmed on the network, adding to queue."
          );

          queuesService.createQueue(
            new QueueDto(
              addressMap.get(account).equals(botUserId)
                ? null
                : addressMap.get(account).getUserId(),
              response.getString("block_account"),
              account,
              LevelDto.RECEIVE,
              hash,
              response.getString("amount"),
              ticker,
              false,
              addressMap.get(account).equals(botUserId)
                ? botUserSeed
                : addressMap.get(account).getSeed(),
              new java.util.Date(),
              null
            )
          );

          fileLogger.info(
            "Added transaction " +
            hash +
            " for account " +
            account +
            " to the queue for ticker " +
            ticker
          );
        }
      }
    } catch (JSONException e) {
      fileLogger.error("Failed to parse receivable JSON: " + e.getMessage());
    }
  }
}
