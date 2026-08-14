package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.util.Constants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServicesImplTest {

  @Mock
  private ActivitiesService activitiesService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private DropsService dropsService;

  @Mock
  private TransferService transferService;

  @Mock
  private TransferExecutorService transferExecutorService;

  private TransferServicesImpl transferServices;

  @BeforeEach
  void setUp() throws Exception {
    CoreServices coreServices = new CoreServices(
      commandsService,
      guildConfigurationsService,
      userDetailsService
    );
    transferServices = new TransferServicesImpl(
      activitiesService,
      coreServices,
      creaturesService,
      currenciesService,
      dropsService,
      transferService
    );

    Field executorField = TransferServicesImpl.class.getDeclaredField(
      "transferExecutorService"
    );
    executorField.setAccessible(true);
    executorField.set(transferServices, transferExecutorService);
  }

  @Test
  void dropReturnsConfirmationWhenTransferIsPriceyAndUnconfirmed() {
    stubBaseAccessForCommand(Constants.COMMAND_NAME_DROP, "user-1", null, true);
    stubCurrenciesAndCreatures();

    TransferDto priceyTransfer = transferWithWallet("XNO", "10");
    priceyTransfer.setPricey(true);
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
      .thenReturn(priceyTransfer);

    TransferResponseDto result = transferServices.drop(dropRequest(false, 0, null));

    assertTrue(result.getConfirmation());
    assertNotNull(result.getDrop());
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
  }

  @Test
  void dropReturnsErrorWhenRandomWinnersExceedJoinLimit() {
    stubBaseAccessForCommand(Constants.COMMAND_NAME_DROP, "user-1", null, true);
    stubCurrenciesAndCreatures();
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
      .thenReturn(transferWithWallet("XNO", "10"));

    TransferResponseDto result = transferServices.drop(dropRequest(true, 2, 3));

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("cannot exceed the number of users"));
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
  }

  @Test
  void giftExecutesTransferAndTracksActivityOnSuccess() {
    stubBaseAccessForCommand(Constants.COMMAND_NAME_GIFT, "user-1", null, true);
    stubCurrenciesAndCreatures();
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
      .thenReturn(transferWithWallet("XNO", "12"));

    TransferResponseDto executed = new TransferResponseDto();
    executed.setCompletedPrimaryTransfers(
      Optional.of(Map.of("receiver-1", transferWithWallet("XNO", "12")))
    );
    when(
      transferExecutorService.executeTransfer(
        eq(Constants.COMMAND_NAME_GIFT),
        anyString(),
        anyString(),
        anyString(),
        isNull(),
        anyList(),
        isNull(),
        any(TransferResponseDto.class)
      )
    )
      .thenReturn(executed);

    TransferResponseDto result = transferServices.gift(giftRequest());

    assertSame(executed, result);
    verify(activitiesService)
      .updateOrCreateActivity(eq("guild-1"), eq("channel-1"), eq("user-1"));
    verify(transferExecutorService)
      .executeTransfer(
        eq(Constants.COMMAND_NAME_GIFT),
        eq("guild-1"),
        eq("channel-1"),
        eq("user-1"),
        isNull(),
        eq(List.of("receiver-1")),
        isNull(),
        any(TransferResponseDto.class)
      );
  }

  @Test
  void mergeReturnsErrorWhenSubordinateIsMissing() {
    stubBaseAccessForCommand(Constants.COMMAND_NAME_MERGE, "user-1", "", false);

    TransferResponseDto result = transferServices.merge(mergeRequest(true));

    assertEquals(
      "You do not have a subordinate user attached to your account.",
      result.getErrorMessage()
    );
    verify(commandsService, never()).setCommands(anyString(), anyString(), any());
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
  }

  @Test
  void sellReturnsErrorWhenTransferContainsWallets() {
    stubBaseAccessForCommand(Constants.COMMAND_NAME_SELL, "user-1", null, true);
    stubCurrenciesAndCreatures();

    TransferDto transferWithWallet = transferWithWallet("XNO", "10");
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
      .thenReturn(transferWithWallet);

    RequestDto sellRequest = new RequestDto(
      null,
      "channel-1",
      true,
      null,
      0,
      false,
      "guild-1",
      null,
      "5 shrimps",
      "",
      null,
      null,
      "user-1",
      null,
      null,
      0
    );

    TransferResponseDto result = transferServices.sell(sellRequest);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("Please specify a **number** and a **creature**"),
      "Expected sell wallet error: " + result.getErrorMessage()
    );
    verify(transferExecutorService, never())
      .executeTransfer(anyString(), anyString(), anyString(), anyString(), any(), any(), any(), any());
  }

  private void stubBaseAccessForCommand(
    String commandName,
    String userId,
    String subordinateUserId,
    boolean includeCommandStub
  ) {
    when(guildConfigurationsService.setGuildConfigurations(anyString(), any()))
      .thenAnswer(invocation -> {
        Consumer<GuildConfigurationsDto> consumer = invocation.getArgument(1);
        consumer.accept(new GuildConfigurationsDto());
        return true;
      });

    when(userDetailsService.setUserDetails(eq(userId), any(), eq(true)))
      .thenAnswer(invocation -> {
        Consumer<UserDetailsDto> consumer = invocation.getArgument(1);
        UserDetailsDto userDetails = new UserDetailsDto();
        userDetails.setUserId(userId);
        userDetails.setSubordinateUserId(subordinateUserId);
        consumer.accept(userDetails);
        return true;
      });

    if (includeCommandStub) {
      when(commandsService.setCommands(eq(commandName), eq(userId), any()))
        .thenAnswer(invocation -> {
          Consumer<List<CommandDto>> consumer = invocation.getArgument(2);
          consumer.accept(List.of(new CommandDto(commandName, "cmd-" + commandName)));
          return true;
        });
    }
  }

  private void stubCurrenciesAndCreatures() {
    doAnswer(invocation -> {
      Consumer<List<CurrencyDto>> consumer = invocation.getArgument(0);
      CurrencyDto currency = new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30");
      currency.setMinimumDrop("1");
      currency.setMinimumGift("1");
      currency.setMinimumRain("1");
      consumer.accept(List.of(currency));
      return null;
    })
      .when(currenciesService).setCurrencies(any());

    doAnswer(invocation -> {
      Consumer<List<CreatureDto>> consumer = invocation.getArgument(0);
      consumer.accept(Collections.emptyList());
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

  private RequestDto dropRequest(boolean confirmation, int users, Integer random) {
    return new RequestDto(
      null,
      "channel-1",
      confirmation,
      null,
      0,
      false,
      "guild-1",
      "role-1",
      "10 xno",
      "",
      random,
      List.of("receiver-1"),
      "user-1",
      null,
      null,
      users
    );
  }

  private RequestDto giftRequest() {
    return new RequestDto(
      null,
      "channel-1",
      true,
      null,
      0,
      false,
      "guild-1",
      null,
      "12 xno",
      "",
      null,
      List.of("receiver-1"),
      "user-1",
      null,
      null,
      0
    );
  }

  private RequestDto mergeRequest(boolean confirmation) {
    return new RequestDto(
      null,
      "channel-1",
      confirmation,
      null,
      0,
      false,
      "guild-1",
      null,
      null,
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
