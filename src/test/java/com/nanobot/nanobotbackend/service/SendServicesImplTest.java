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
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.util.Constants;
import java.lang.reflect.Field;
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
    assertTrue(result.getErrorMessage().contains("valid address for Nano"));
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
