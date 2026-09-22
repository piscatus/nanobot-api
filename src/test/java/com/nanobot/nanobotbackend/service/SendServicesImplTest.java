package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.service.chain.ChainAdapter;
import com.nanobot.nanobotbackend.service.chain.ChainAdapterRegistry;
import com.nanobot.nanobotbackend.service.chain.FeeQuote;
import com.nanobot.nanobotbackend.service.chain.WalletRefusal;
import com.nanobot.nanobotbackend.util.Constants;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendServicesImplTest {

  private static final String VALID_SEED =
    "0000000000000000000000000000000000000000000000000000000000000000";
  private static final String VALID_NANO_ADDRESS = "nano_1" + "a".repeat(59);

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private QueuesService queuesService;

  @Mock
  private TransferService transferService;

  @Mock
  private TransferExecutorService transferExecutorService;

  @Mock
  private ChainAdapterRegistry chainAdapterRegistry;

  private SendServicesImpl sendServices;

  @BeforeEach
  void setUp() throws Exception {
    CoreServices coreServices = new CoreServices(
      commandsService,
      guildConfigurationsService,
      userDetailsService
    );
    sendServices = new SendServicesImpl(
      coreServices,
      currenciesService,
      creaturesService,
      queuesService,
      transferService
    );

    Field executorField = SendServicesImpl.class.getDeclaredField(
      "transferExecutorService"
    );
    executorField.setAccessible(true);
    executorField.set(sendServices, transferExecutorService);

    Field registryField = SendServicesImpl.class.getDeclaredField(
      "chainAdapterRegistry"
    );
    registryField.setAccessible(true);
    registryField.set(sendServices, chainAdapterRegistry);
  }

  @Test
  void sendRequiresConfirmationForValidUnconfirmedRequest() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    TransferResponseDto result = sendServices.send(
      sendRequest(false, VALID_NANO_ADDRESS)
    );

    assertTrue(result.getConfirmation());
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
    verify(queuesService, never()).createQueue(any());
  }

  @Test
  void sendRejectsInvalidAddressForCurrency() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    TransferResponseDto result = sendServices.send(sendRequest(true, "not-a-nano-address"));

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("valid Nano (XNO) address"));
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
  }

  @Test
  void sendConfirmedRequestExecutesTransferAndCreatesQueue() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "55"));

    UserDetailsEntity bot = botUser("0");
    when(userDetailsService.getUserDetailsByUserId(any())).thenReturn(Optional.of(bot));

    TransferResponseDto executed = new TransferResponseDto();
    executed.setCompletedPrimaryTransfers(
      Optional.of(Map.of("0", transferWithWallet("XNO", "55")))
    );
    executed.setTransactionId("tx-1");
    when(transferExecutorService.executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any()))
      .thenReturn(executed);

    QueueEntity queued = new QueueEntity();
    queued.setId("queue-1");
    when(queuesService.createQueue(any())).thenReturn(queued);

    TransferResponseDto result = sendServices.send(
      sendRequest(true, VALID_NANO_ADDRESS)
    );

    assertNull(result.getErrorMessage());
    verify(transferExecutorService)
      .executeTransfer(
        eq(Constants.COMMAND_NAME_SEND),
        eq("guild-1"),
        eq("channel-1"),
        eq("user-1"),
        isNull(),
        eq(List.of("0")),
        isNull(),
        any(TransferResponseDto.class)
      );

    ArgumentCaptor<QueueDto> queueCaptor = ArgumentCaptor.forClass(QueueDto.class);
    verify(queuesService).createQueue(queueCaptor.capture());
    assertEquals(LevelDto.SEND, queueCaptor.getValue().getLevel());
    assertEquals("55", queueCaptor.getValue().getRaw());
    assertEquals("XNO", queueCaptor.getValue().getTicker());
    assertEquals(VALID_NANO_ADDRESS, queueCaptor.getValue().getTargetAddress());
    assertEquals("tx-1", queueCaptor.getValue().getTransactionId());
  }

  /**
   * Without an estimate the effective minimum quietly drops to the policy floor
   * alone, which on a fee-bearing network accepts a withdrawal the fee will eat.
   * Refusing up front beats debiting the user and refunding them later.
   */
  @Test
  void sendRejectsFeeBearingCurrencyWithNoFeeEstimate() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    ChainAdapter feeBearing = mock(ChainAdapter.class);
    when(feeBearing.hasNetworkFee()).thenReturn(true);
    when(chainAdapterRegistry.getByProtocol(any()))
      .thenReturn(Optional.of(feeBearing));

    TransferResponseDto result = sendServices.send(
      sendRequest(true, VALID_NANO_ADDRESS)
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("network fee"));
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
    verify(queuesService, never()).createQueue(any());
  }

  /**
   * The stored estimate assumes a transaction size. The confirmation should
   * carry the fee the wallet actually quoted for this withdrawal.
   */
  @Test
  void sendCarriesTheWalletsFeeQuoteOnTheConfirmationPass() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker("XNO");
    ChainAdapter adapter = mock(ChainAdapter.class);
    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(entity));
    when(chainAdapterRegistry.get(entity)).thenReturn(Optional.of(adapter));
    when(adapter.quoteWithdrawalFee(entity, "25", VALID_NANO_ADDRESS))
      .thenReturn(FeeQuote.quoted(BigInteger.valueOf(7)));

    TransferResponseDto result = sendServices.send(
      sendRequest(false, VALID_NANO_ADDRESS)
    );

    assertNull(result.getErrorMessage());
    assertTrue(result.getConfirmation());
    assertEquals("7", result.getNetworkFee());
    verify(queuesService, never()).createQueue(any());
  }

  /**
   * A withdrawal the wallet cannot build would otherwise be debited, fail three
   * times in the queue and be refunded. Saying so at confirmation is cheaper
   * for everyone.
   */
  @Test
  void sendRejectsAWithdrawalTheWalletRefusesToBuild() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker("XNO");
    ChainAdapter adapter = mock(ChainAdapter.class);
    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(entity));
    when(chainAdapterRegistry.get(entity)).thenReturn(Optional.of(adapter));
    when(adapter.quoteWithdrawalFee(entity, "25", VALID_NANO_ADDRESS))
      .thenReturn(
        FeeQuote.rejected(
          new WalletRefusal(
            WalletRefusal.Kind.FEE_EXCEEDS_AMOUNT,
            "The network fee would exceed the amount requested."
          )
        )
      );

    TransferResponseDto result = sendServices.send(
      sendRequest(false, VALID_NANO_ADDRESS)
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("network fee would exceed"));
    assertTrue(result.getErrorMessage().contains("Nothing has been debited"));
    assertFalse(result.getConfirmation());
    assertNull(result.getNetworkFee());
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
  }

  /** The quote improves the confirmation; it must never block a withdrawal. */
  @Test
  void sendStillConfirmsWhenNoQuoteIsAvailable() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker("XNO");
    ChainAdapter adapter = mock(ChainAdapter.class);
    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(entity));
    when(chainAdapterRegistry.get(entity)).thenReturn(Optional.of(adapter));
    when(adapter.quoteWithdrawalFee(entity, "25", VALID_NANO_ADDRESS))
      .thenReturn(FeeQuote.unavailable());

    TransferResponseDto result = sendServices.send(
      sendRequest(false, VALID_NANO_ADDRESS)
    );

    assertNull(result.getErrorMessage());
    assertTrue(result.getConfirmation());
    assertNull(result.getNetworkFee());
  }

  /**
   * Locked funds are still sendable, but only if the user opts in to waiting.
   * The preview confirms with a delay notice and does not debit.
   */
  @Test
  void sendAsksBeforeQueueingALockedWithdrawal() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "25"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker("XNO");
    ChainAdapter adapter = mock(ChainAdapter.class);
    when(currenciesService.getCurrencyByTicker("XNO"))
      .thenReturn(Optional.of(entity));
    when(chainAdapterRegistry.get(entity)).thenReturn(Optional.of(adapter));
    when(adapter.quoteWithdrawalFee(entity, "25", VALID_NANO_ADDRESS))
      .thenReturn(
        FeeQuote.delayed(
          new WalletRefusal(
            WalletRefusal.Kind.FUNDS_LOCKED,
            "The bot's Monero wallet is briefly locked."
          )
        )
      );

    TransferResponseDto result = sendServices.send(
      sendRequest(false, VALID_NANO_ADDRESS)
    );

    assertNull(result.getErrorMessage());
    assertTrue(result.getConfirmation());
    assertTrue(result.getDelayed());
    assertEquals(
      "The bot's Monero wallet is briefly locked.",
      result.getDelayNotice()
    );
    assertNull(result.getNetworkFee());
    verify(queuesService, never()).createQueue(any());
  }

  /**
   * The debit and the queue insert are separate writes. If the second one does
   * not land, nothing downstream will ever send or refund the withdrawal, so
   * the balance has to be put back here.
   */
  @Test
  void sendReturnsTheDebitWhenTheQueueEntryCannotBeCreated() {
    stubCoreAccess("user-1");
    stubCurrencies(true);
    stubCreatures();
    when(
      transferService.processInputs(
        anyString(),
        any(),
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyBoolean()
      )
    )
      .thenReturn(transferWithWallet("XNO", "55"));
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    TransferResponseDto executed = new TransferResponseDto();
    executed.setCompletedPrimaryTransfers(
      Optional.of(Map.of("0", transferWithWallet("XNO", "55")))
    );
    executed.setTransactionId("tx-1");

    TransferResponseDto refunded = new TransferResponseDto();
    refunded.setTransactionId("refund-1");

    when(transferExecutorService.executeTransfer(anyString(), any(), any(), anyString(), any(), any(), any(), any()))
      .thenReturn(executed)
      .thenReturn(refunded);

    when(queuesService.createQueue(any())).thenReturn(null);

    TransferResponseDto result = sendServices.send(
      sendRequest(true, VALID_NANO_ADDRESS)
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("returned to your balance"));

    // The debit, then the compensating credit back from the system account.
    ArgumentCaptor<String> senderCaptor = ArgumentCaptor.forClass(String.class);
    verify(transferExecutorService, times(2))
      .executeTransfer(
        anyString(),
        any(),
        any(),
        senderCaptor.capture(),
        any(),
        any(),
        any(),
        any()
      );
    assertEquals(List.of("user-1", "0"), senderCaptor.getAllValues());
  }

  @Test
  void updateReturnsErrorWhenWithdrawalsAreDisabledForMatchingAddress() {
    stubCoreAccess("user-1");
    stubCurrencies(false);
    when(userDetailsService.getUserDetailsByUserId(any()))
      .thenReturn(Optional.of(botUser("0")));

    TransferResponseDto result = sendServices.update(
      sendRequest(true, VALID_NANO_ADDRESS)
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("updates are currently disabled"));
    verify(queuesService, never()).createQueue(any());
  }

  private void stubCoreAccess(String userId) {
    when(guildConfigurationsService.setGuildConfigurations(anyString(), any()))
      .thenAnswer(invocation -> {
        Consumer<GuildConfigurationsDto> consumer = invocation.getArgument(1);
        consumer.accept(new GuildConfigurationsDto());
        return true;
      });

    when(userDetailsService.setUserDetails(eq(userId), any(), eq(true)))
      .thenAnswer(invocation -> {
        Consumer<UserDetailsDto> consumer = invocation.getArgument(1);
        UserDetailsDto dto = new UserDetailsDto();
        dto.setUserId(userId);
        consumer.accept(dto);
        return true;
      });

    when(commandsService.setCommands(eq(Constants.COMMAND_NAME_SEND), eq(userId), any()))
      .thenAnswer(invocation -> {
        Consumer<List<CommandDto>> consumer = invocation.getArgument(2);
        consumer.accept(List.of(new CommandDto(Constants.COMMAND_NAME_SEND, "cmd-send")));
        return true;
      });
  }

  private void stubCurrencies(boolean withdrawalsEnabled) {
    doAnswer(invocation -> {
      Consumer<List<CurrencyDto>> consumer = invocation.getArgument(0);
      CurrencyDto xno = new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30");
      xno.setAddress("^nano_[13][13456789abcdefghijkmnopqrstuwxyz]{59}$");
      xno.setProcessWithdrawals(withdrawalsEnabled);
      consumer.accept(List.of(xno));
      return null;
    })
      .when(currenciesService).setCurrencies(any());
  }

  private void stubCreatures() {
    doAnswer(invocation -> {
      Consumer<List<CreatureDto>> consumer = invocation.getArgument(0);
      consumer.accept(new ArrayList<>());
      return null;
    })
      .when(creaturesService).setCreatures(any());
  }

  private TransferDto transferWithWallet(String ticker, String raw) {
    return new TransferDto(
      new ArrayList<>(List.of(new WalletDto(ticker, raw))),
      new ArrayList<>()
    );
  }

  private UserDetailsEntity botUser(String userId) {
    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setUserId(userId);
    entity.setSeed(VALID_SEED);
    return entity;
  }

  private RequestDto sendRequest(boolean confirmation, String address) {
    return new RequestDto(
      address,
      "channel-1",
      confirmation,
      null,
      0,
      false,
      "guild-1",
      null,
      "1 xno",
      "",
      null,
      null,
      "user-1",
      null,
      null,
      0
    );
  }
}
