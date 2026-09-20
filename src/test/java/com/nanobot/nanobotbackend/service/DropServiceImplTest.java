package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TriviaQuestionDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DropServiceImplTest {

  @Mock
  private CommandsService commandsService;

  @Mock
  private CoreServices coreServices;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private DropsService dropsService;

  @Mock
  private MessagesService messagesService;

  @Mock
  private PickupsService pickupsService;

  @Mock
  private TransferExecutorService transferExecutorService;

  private DropServiceImpl dropService;

  @BeforeEach
  void setUp() {
    dropService = new DropServiceImpl(coreServices, currenciesService, dropsService, messagesService, pickupsService);
    // Inject TransferExecutorService via reflection since it's @Autowired
    try {
      var field = DropServiceImpl.class.getDeclaredField("transferExecutorService");
      field.setAccessible(true);
      field.set(dropService, transferExecutorService);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void formatPickeruppersShouldReturnNoOneWhenEmpty() {
    DropEntity drop = new DropEntity();
    drop.setUserId("u1");
    drop.setInput("100");
    drop.setMessageData("NANO");
    List<PickupDto> pickeruppers = new ArrayList<>();

    String result = dropService.formatPickeruppers("drop-cmd-id", drop, pickeruppers);

    assertEquals(
      "<@u1> used </drop:drop-cmd-id> to transfer **100**\nNANO to **no one**!",
      result
    );
  }

  @Test
  void formatPickeruppersShouldReturnSingleMentionWhenOnePickerupper() {
    DropEntity drop = new DropEntity();
    drop.setUserId("u1");
    drop.setInput("100");
    drop.setMessageData("NANO");
    PickupDto p1 = new PickupDto();
    p1.setUserId("u2");
    List<PickupDto> pickeruppers = List.of(p1);

    String result = dropService.formatPickeruppers("drop-cmd-id", drop, pickeruppers);

    assertEquals(
      "<@u1> used </drop:drop-cmd-id> to transfer **100**\nNANO to <@u2>!",
      result
    );
  }

  @Test
  void formatPickeruppersShouldReturnTwoMentionsWithAndWhenTwoPickeruppers() {
    DropEntity drop = new DropEntity();
    drop.setUserId("u1");
    drop.setInput("100");
    drop.setMessageData("NANO");
    PickupDto p1 = new PickupDto();
    p1.setUserId("u2");
    PickupDto p2 = new PickupDto();
    p2.setUserId("u3");
    List<PickupDto> pickeruppers = List.of(p1, p2);

    String result = dropService.formatPickeruppers("drop-cmd-id", drop, pickeruppers);

    assertEquals(
      "<@u1> used </drop:drop-cmd-id> to transfer **100**\nNANO to <@u2> and <@u3>!",
      result
    );
  }

  @Test
  void formatPickeruppersShouldReturnCommaSeparatedWhenThreeOrMore() {
    DropEntity drop = new DropEntity();
    drop.setUserId("u1");
    drop.setInput("100");
    drop.setMessageData("NANO");
    PickupDto p1 = new PickupDto();
    p1.setUserId("u2");
    PickupDto p2 = new PickupDto();
    p2.setUserId("u3");
    PickupDto p3 = new PickupDto();
    p3.setUserId("u4");
    List<PickupDto> pickeruppers = List.of(p1, p2, p3);

    String result = dropService.formatPickeruppers("drop-cmd-id", drop, pickeruppers);

    assertEquals(
      "<@u1> used </drop:drop-cmd-id> to transfer **100**\nNANO to <@u2>, <@u3>, and <@u4>!",
      result
    );
  }

  @Test
  void dropUpdateShouldReturnSuccessWhenValidRequest() {
    RequestDto request = createRequestDto("drop1", "msg1");
    DropEntity updated = new DropEntity();
    updated.setId("drop1");
    when(dropsService.updateDropMessageId(any())).thenReturn(Optional.of(updated));

    var result = dropService.dropUpdate(request);

    assertNull(result.getErrorMessage());
    verify(dropsService).updateDropMessageId(any());
  }

  @Test
  void dropUpdateShouldReturnErrorWhenInvalidId() {
    RequestDto request = createRequestDto("", "msg1");

    var result = dropService.dropUpdate(request);

    assertEquals("Drop failed to update.", result.getErrorMessage());
  }

  @Test
  void dropUpdateShouldReturnErrorWhenInvalidDropId() {
    RequestDto request = createRequestDto("drop1", "0");

    var result = dropService.dropUpdate(request);

    assertEquals("Drop failed to update.", result.getErrorMessage());
  }

  @Test
  void dropUpdateShouldReturnErrorWhenUpdateReturnsEmpty() {
    RequestDto request = createRequestDto("drop1", "msg1");
    when(dropsService.updateDropMessageId(any())).thenReturn(Optional.empty());

    var result = dropService.dropUpdate(request);

    assertEquals("Drop failed to update.", result.getErrorMessage());
  }

  @Test
  void dropUpdateShouldReturnErrorWhenIdNull() {
    RequestDto request = createRequestDto(null, "msg1");

    var result = dropService.dropUpdate(request);

    assertEquals("Drop failed to update.", result.getErrorMessage());
    verify(dropsService, never()).updateDropMessageId(any());
  }

  @Test
  void formatPickeruppersShouldTruncateWhenMoreThan40() {
    DropEntity drop = new DropEntity();
    drop.setUserId("u1");
    drop.setInput("100");
    drop.setMessageData("NANO");
    List<PickupDto> pickeruppers = new ArrayList<>();
    for (int i = 0; i < 45; i++) {
      PickupDto p = new PickupDto();
      p.setUserId("u" + (i + 2));
      pickeruppers.add(p);
    }

    String result = dropService.formatPickeruppers("drop-cmd-id", drop, pickeruppers);

    assertTrue(result.contains("<@u2>"));
    assertTrue(result.contains(" and 5 others!"));
    assertFalse(result.contains("<@u48>"));
  }

  @Test
  void checkDropsByPickupSizeShouldReturnEarlyWhenNoDrops() {
    when(dropsService.getDrops(null)).thenReturn(List.of());

    dropService.checkDropsByPickupSize();

    verify(dropsService).getDrops(null);
    verify(pickupsService, never()).getPickups(any(), any());
  }

  @Test
  void checkDropsByTimeShouldReturnEarlyWhenNoDrops() {
    when(dropsService.getDrops(null)).thenReturn(List.of());

    dropService.checkDropsByTime();

    verify(dropsService).getDrops(null);
    verify(pickupsService, never()).getPickups(any(), any());
  }

  @Test
  void checkDropsByPickupSizeSkipsWhenMessageIdNull() {
    DropEntity drop = new DropEntity();
    drop.setId("d1");
    drop.setMessageId(null);
    drop.setMaximumEntries("2");
    when(dropsService.getDrops(null)).thenReturn(List.of(drop));

    dropService.checkDropsByPickupSize();

    verify(pickupsService, never()).getPickups(any(), any());
  }

  @Test
  void checkDropsByPickupSizeSkipsWhenPickupsBelowMax() {
    DropEntity drop = new DropEntity();
    drop.setId("d1");
    drop.setMessageId("msg1");
    drop.setMaximumEntries("10");
    when(dropsService.getDrops(null)).thenReturn(List.of(drop));
    when(pickupsService.getPickups("msg1", null)).thenReturn(List.of(new PickupEntity()));

    dropService.checkDropsByPickupSize();

    verify(dropsService, never()).deleteDrop(any());
  }

  @Test
  void dropCleanupDoesNothingWhenDeleteDropReturnsEmpty() {
    DropEntity drop = createMinimalDrop();
    when(dropsService.deleteDrop("drop-1")).thenReturn(Optional.empty());

    dropService.dropCleanup(drop, List.of());

    verify(dropsService).deleteDrop("drop-1");
    verify(transferExecutorService, never()).executeTransfer(any(), any(), any(), any(), any(), any(), any(), any());
    verify(messagesService, never()).createMessage(any(MessageDto.class));
  }

  @Test
  void dropCleanupWithEmptyPickupsCallsExecuteTransferAndCreateMessage() throws Exception {
    DropEntity drop = createMinimalDrop();
    drop.setTransfer(new TransferDto());
    when(dropsService.deleteDrop("drop-1")).thenReturn(Optional.of(drop));
    when(commandsService.setCommands(eq(Constants.COMMAND_NAME_DROP), any(), any()))
      .thenAnswer(inv -> {
        inv.getArgument(2, java.util.function.Consumer.class).accept(
          List.of(new com.nanobot.nanobotbackend.dto.CommandDto("drop", "drop-cmd-id"))
        );
        return true;
      });

    DropServiceImpl serviceWithCore = new DropServiceImpl(
      new com.nanobot.nanobotbackend.service.CoreServices(
        commandsService,
        org.mockito.Mockito.mock(com.nanobot.nanobotbackend.service.GuildConfigurationsService.class),
        org.mockito.Mockito.mock(com.nanobot.nanobotbackend.service.UserDetailsService.class)
      ),
      currenciesService,
      dropsService,
      messagesService,
      pickupsService
    );
    var field = DropServiceImpl.class.getDeclaredField("transferExecutorService");
    field.setAccessible(true);
    field.set(serviceWithCore, transferExecutorService);

    serviceWithCore.dropCleanup(drop, List.of());

    verify(dropsService).deleteDrop("drop-1");
    verify(commandsService).setCommands(eq(Constants.COMMAND_NAME_DROP), any(), any());
    verify(transferExecutorService).executeTransfer(
      eq(Constants.COMMAND_NAME_DROP),
      eq("g1"),
      eq("ch1"),
      eq("0"),
      eq(null),
      eq(List.of("u1")),
      eq(null),
      any()
    );
    verify(messagesService).createMessage(any(MessageDto.class));
  }

  @Test
  void dropCleanupWithPickupsCallsDeletePickupExecuteTransferAndCreateMessage() throws Exception {
    DropEntity drop = createMinimalDrop();
    drop.setTransfer(new TransferDto());
    drop.setNumberWinners(0);
    PickupEntity pickup = new PickupEntity();
    pickup.setId("pickup-1");
    pickup.setUserId("u2");
    when(dropsService.deleteDrop("drop-1")).thenReturn(Optional.of(drop));
    when(commandsService.setCommands(eq(Constants.COMMAND_NAME_DROP), any(), any()))
      .thenAnswer(inv -> {
        inv.getArgument(2, java.util.function.Consumer.class).accept(
          List.of(new com.nanobot.nanobotbackend.dto.CommandDto("drop", "drop-cmd-id"))
        );
        return true;
      });
    when(pickupsService.deletePickup("pickup-1")).thenReturn(Optional.of(pickup));

    DropServiceImpl serviceWithCore = new DropServiceImpl(
      new com.nanobot.nanobotbackend.service.CoreServices(
        commandsService,
        org.mockito.Mockito.mock(com.nanobot.nanobotbackend.service.GuildConfigurationsService.class),
        org.mockito.Mockito.mock(com.nanobot.nanobotbackend.service.UserDetailsService.class)
      ),
      currenciesService,
      dropsService,
      messagesService,
      pickupsService
    );
    var field = DropServiceImpl.class.getDeclaredField("transferExecutorService");
    field.setAccessible(true);
    field.set(serviceWithCore, transferExecutorService);

    serviceWithCore.dropCleanup(drop, List.of(pickup));

    verify(dropsService).deleteDrop("drop-1");
    verify(pickupsService).deletePickup("pickup-1");
    verify(transferExecutorService).executeTransfer(
      eq(Constants.COMMAND_NAME_DROP),
      eq("g1"),
      eq("ch1"),
      eq("0"),
      eq("u1"),
      eq(List.of("u1")),
      eq(List.of("u2")),
      any()
    );
    verify(messagesService).createMessage(any(MessageDto.class));
  }

  @Test
  void selectTriviaWinnersOrdersByTimestampCutsToMaximumEntriesAndIgnoresWrongAnswers() {
    DropEntity drop = triviaDropWithCorrectIndex(2);
    drop.setMaximumEntries("2");

    PickupEntity wrong = triviaPickup("wrong", 0, new Date(1_000));
    PickupEntity late = triviaPickup("late", 2, new Date(4_000));
    PickupEntity early = triviaPickup("early", 2, new Date(2_000));
    PickupEntity middle = triviaPickup("middle", 2, new Date(3_000));

    List<PickupEntity> winners = DropServiceImpl.selectTriviaWinners(
      drop,
      List.of(wrong, late, early, middle)
    );

    assertEquals(2, winners.size());
    assertEquals("early", winners.get(0).getUserId());
    assertEquals("middle", winners.get(1).getUserId());
  }

  @Test
  void selectTriviaWinnersReturnsAllWhenMaximumEntriesExceedsCorrectCount() {
    DropEntity drop = triviaDropWithCorrectIndex(2);
    drop.setMaximumEntries("10");

    PickupEntity first = triviaPickup("a", 2, new Date(1_000));
    PickupEntity second = triviaPickup("b", 2, new Date(2_000));
    PickupEntity wrong = triviaPickup("c", 0, new Date(1_500));

    List<PickupEntity> winners = DropServiceImpl.selectTriviaWinners(
      drop,
      List.of(first, second, wrong)
    );

    assertEquals(2, winners.size());
    assertEquals("a", winners.get(0).getUserId());
    assertEquals("b", winners.get(1).getUserId());
  }

  @Test
  void correctPickupsReturnsEverythingForANonTriviaDrop() {
    DropEntity drop = new DropEntity();
    PickupEntity first = triviaPickup("a", 0, new Date(1_000));
    PickupEntity second = triviaPickup("b", 2, new Date(2_000));
    List<PickupEntity> pickups = List.of(first, second);

    List<PickupEntity> result = DropServiceImpl.correctPickups(drop, pickups);

    assertEquals(2, result.size());
    assertEquals("a", result.get(0).getUserId());
    assertEquals("b", result.get(1).getUserId());
  }

  private DropEntity createMinimalDrop() {
    DropEntity drop = new DropEntity();
    drop.setId("drop-1");
    drop.setUserId("u1");
    drop.setGuildId("g1");
    drop.setChannelId("ch1");
    drop.setMessageId("msg-1");
    drop.setDuration(5);
    drop.setInput("100");
    drop.setMessageData("NANO");
    drop.setMaximumEntries("10");
    return drop;
  }

  private RequestDto createRequestDto(String id, String dropId) {
    RequestDto dto = new RequestDto(
      null,
      null,
      false,
      dropId,
      null,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    dto.setId(id);
    return dto;
  }

  private DropEntity triviaDropWithCorrectIndex(int correctIndex) {
    DropEntity drop = new DropEntity();
    TriviaQuestionDto trivia = new TriviaQuestionDto();
    trivia.setAnswers(List.of("A", "B", "C", "D"));
    trivia.setCorrectIndex(correctIndex);
    drop.setTrivia(trivia);
    return drop;
  }

  private PickupEntity triviaPickup(
    String userId,
    int answerIndex,
    Date timestamp
  ) {
    PickupEntity pickup = new PickupEntity();
    pickup.setUserId(userId);
    pickup.setAnswerIndex(answerIndex);
    pickup.setTimestamp(timestamp);
    return pickup;
  }
}
