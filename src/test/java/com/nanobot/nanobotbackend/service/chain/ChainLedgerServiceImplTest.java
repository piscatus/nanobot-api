package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.service.CommandsService;
import com.nanobot.nanobotbackend.service.CoreServices;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.GuildConfigurationsService;
import com.nanobot.nanobotbackend.service.MessagesService;
import com.nanobot.nanobotbackend.service.TransactionsService;
import com.nanobot.nanobotbackend.service.TransferExecutorService;
import com.nanobot.nanobotbackend.service.UserDetailsService;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ChainLedgerServiceImplTest {

  private CurrenciesService currenciesService;
  private MessagesService messagesService;
  private TransactionsService transactionsService;
  private TransferExecutorService transferExecutorService;
  private ChainLedgerServiceImpl service;

  private final Map<String, String> commandMap = Map.of(
    "transactions",
    "t1",
    "wallet",
    "w1"
  );

  @BeforeEach
  void setUp() {
    CoreServices coreServices = new CoreServices(
      mock(CommandsService.class),
      mock(GuildConfigurationsService.class),
      mock(UserDetailsService.class)
    );
    currenciesService = mock(CurrenciesService.class);
    messagesService = mock(MessagesService.class);
    transactionsService = mock(TransactionsService.class);
    transferExecutorService = mock(TransferExecutorService.class);

    when(currenciesService.getCurrencyDecimalValue(anyString(), anyInt()))
      .thenReturn("1.5");
    when(currenciesService.getCurrencyDollarValue(anyString(), any()))
      .thenReturn("300.00");

    service = new ChainLedgerServiceImpl(
      coreServices,
      currenciesService,
      messagesService,
      transactionsService
    );
    ReflectionTestUtils.setField(
      service,
      "transferExecutorService",
      transferExecutorService
    );
  }

  private static CurrencyEntity currency(String protocol) {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker(protocol.equals(Constants.PROTOCOL_NANO) ? "XNO" : "XMR");
    currency.setName(protocol.equals(Constants.PROTOCOL_NANO) ? "Nano" : "Monero");
    currency.setProtocol(protocol);
    currency.setPrecision("12");
    currency.setEmoji("E");
    currency.setColor("#000000");
    currency.setValue("200");
    return currency;
  }

  private static QueueEntity queue(String userId) {
    QueueEntity queue = new QueueEntity();
    queue.setUserId(userId);
    queue.setRaw("1500000000000");
    queue.setBlockHash("abc123");
    queue.setTargetAddress("dest");
    queue.setTransactionId("tx-1");
    return queue;
  }

  private List<MessageDto> capturedMessages() {
    ArgumentCaptor<MessageDto> captor = ArgumentCaptor.forClass(
      MessageDto.class
    );
    verify(messagesService, org.mockito.Mockito.atLeast(0))
      .createMessage(captor.capture());
    return captor.getAllValues();
  }

  private TransferResponseDto successfulTransfer() {
    TransferResponseDto transfer = new TransferResponseDto();
    transfer.setTransactionId("ledger-1");
    return transfer;
  }

  /**
   * The Monero self-withdrawal path relies on this: it credits first, for
   * recoverability, and announces later, for readable ordering. If the quiet
   * credit wrote messages the user would get the deposit notice twice.
   */
  @Test
  void quietCreditShouldMoveValueWithoutWritingMessages() {
    when(
      transferExecutorService.executeTransfer(
        eq(Constants.COMMAND_NAME_RECEIVE),
        isNull(),
        isNull(),
        eq("0"),
        isNull(),
        eq(List.of("user")),
        isNull(),
        any()
      )
    )
      .thenReturn(successfulTransfer());

    String transactionId = service.creditDeposit(
      currency(Constants.PROTOCOL_MONERO),
      "user",
      "1500000000000",
      "txid",
      "addr",
      commandMap,
      false
    );

    assertEquals("ledger-1", transactionId);
    verify(messagesService, never()).createMessage(any());
  }

  @Test
  void creditShouldStillNotifyByDefault() {
    when(
      transferExecutorService.executeTransfer(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    )
      .thenReturn(successfulTransfer());

    service.creditDeposit(
      currency(Constants.PROTOCOL_MONERO),
      "user",
      "1500000000000",
      "txid",
      "addr",
      commandMap
    );

    List<MessageDto> messages = capturedMessages();
    assertEquals(2, messages.size());
    assertTrue(
      messages.stream().allMatch(m -> "🧾 Deposit Confirmed".equals(m.getTitle()))
    );
  }

  @Test
  void failedCreditShouldReturnNullAndSayNothing() {
    when(
      transferExecutorService.executeTransfer(
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    )
      .thenReturn(null);

    assertNull(
      service.creditDeposit(
        currency(Constants.PROTOCOL_MONERO),
        "user",
        "1",
        "txid",
        "addr",
        commandMap
      )
    );
    verify(messagesService, never()).createMessage(any());
  }

  @Test
  void depositConfirmedNoticeShouldGoToLogChannelAndUser() {
    service.notifyDepositConfirmed(
      currency(Constants.PROTOCOL_MONERO),
      "user",
      "1500000000000",
      "txid",
      "addr",
      "ledger-1",
      commandMap
    );

    List<MessageDto> messages = capturedMessages();
    assertEquals(2, messages.size());
    assertNull(messages.get(0).getUserId());
    assertEquals("user", messages.get(1).getUserId());
    assertTrue(messages.get(1).getContent().contains("</wallet:w1>"));
    assertTrue(messages.get(1).getContent().contains("txid"));
  }

  /**
   * Nothing has been credited yet, so the discovered notice must not send the
   * user to /wallet and must say how far along the deposit is.
   */
  @Test
  void depositDiscoveredNoticeShouldQuoteProgressAndSkipWalletPointer() {
    service.notifyDepositDiscovered(
      currency(Constants.PROTOCOL_MONERO),
      "user",
      "1500000000000",
      "txid",
      "addr",
      3L,
      10,
      commandMap
    );

    List<MessageDto> messages = capturedMessages();
    assertEquals(2, messages.size());
    MessageDto userMessage = messages.get(1);
    assertEquals("👀 Deposit Discovered", userMessage.getTitle());
    assertEquals("user", userMessage.getUserId());
    assertTrue(userMessage.getContent().contains("**10 confirmations**"));
    assertTrue(userMessage.getContent().contains("(currently 3)"));
    assertFalse(userMessage.getContent().contains("</wallet:"));
  }

  @Test
  void withdrawalSentNoticeShouldUseChainWording() {
    service.notifyWithdrawalSent(
      currency(Constants.PROTOCOL_MONERO),
      queue("user"),
      10,
      commandMap
    );

    MessageDto userMessage = capturedMessages().get(1);
    assertEquals("📨 Withdrawal Sent", userMessage.getTitle());
    assertTrue(userMessage.getContent().contains("__Transaction__"));
    assertTrue(userMessage.getContent().contains("network fee was deducted"));
    assertTrue(userMessage.getContent().contains("**10 confirmations**"));
  }

  /**
   * Nano is feeless and has blocks rather than transactions, so its notice must
   * not claim a fee was taken and should keep the wording the Nano path already
   * uses. The dev Nano currency has no explorer template configured, so the
   * link falls back to nanexplorer rather than vanishing.
   */
  @Test
  void withdrawalSentNoticeShouldAdaptToNano() {
    service.notifyWithdrawalSent(
      currency(Constants.PROTOCOL_NANO),
      queue("user"),
      1,
      commandMap
    );

    MessageDto userMessage = capturedMessages().get(1);
    assertTrue(userMessage.getContent().contains("__Block Hash__"));
    assertFalse(userMessage.getContent().contains("network fee"));
    assertTrue(userMessage.getContent().contains("**1 confirmation**"));
    assertEquals(
      "https://nanexplorer.com/nano/blocks/abc123",
      userMessage.getUrl()
    );
  }

  @Test
  void withdrawalSentNoticeShouldSkipBotOwnedSends() {
    service.notifyWithdrawalSent(
      currency(Constants.PROTOCOL_MONERO),
      queue(null),
      10,
      commandMap
    );

    verify(messagesService, never()).createMessage(any());
  }

  @Test
  void configuredExplorerTemplateShouldWinOverNanoFallback() {
    CurrencyEntity nano = currency(Constants.PROTOCOL_NANO);
    nano.setExplorerTxUrl("https://example.com/{value}");

    assertEquals(
      "https://example.com/abc",
      service.explorerTxUrl(nano, "abc")
    );
  }

  @Test
  void pluralConfirmationsShouldReadNaturally() {
    assertEquals("1 confirmation", ChainLedgerServiceImpl.pluralConfirmations(1));
    assertEquals(
      "6 confirmations",
      ChainLedgerServiceImpl.pluralConfirmations(6)
    );
  }
}
