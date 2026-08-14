import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.service.AliasesService;
import com.nanobot.nanobotbackend.service.CreaturesService;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.TransferService;
import com.nanobot.nanobotbackend.service.TransferServiceImpl;
import com.nanobot.nanobotbackend.service.UserItemsService;
import com.nanobot.nanobotbackend.service.UserWalletsService;
import com.nanobot.nanobotbackend.util.Constants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

  @Mock
  private AliasesService aliasesService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private UserItemsService userItemsService;

  @Mock
  private UserWalletsService userWalletsService;

  private TransferService transferService;
  private Consumer<String> errorMessageConsumer;
  private Map<String, String> commandMap;

  @BeforeEach
  void setUp() {
    errorMessageConsumer = msg -> {};
    commandMap =
      Map.of(
        Constants.COMMAND_NAME_WALLET,
        "1234567890123456789",
        Constants.COMMAND_NAME_INVENTORY,
        "9876543210987654321"
      );
    transferService =
      new TransferServiceImpl(
        aliasesService,
        creaturesService,
        currenciesService,
        userItemsService,
        userWalletsService
      );
  }

  @Test
  void testValidAliasWithAmount() {
    lenient().when(aliasesService.analyzeAliases(eq("GLOBAL"), eq("big")))
      .thenReturn(
        new AliasDto("GLOBAL", "big", "bigs", "XNO", "10000000000000000000000000000", ":telescope:")
      );
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));

    TransferDto result =
      transferService.processInputs(
        "gift",
        errorMessageConsumer,
        commandMap,
        "GLOBAL",
        null,
        "1 big",
        false
      );

    WalletDto updatedWallet =
      result.getWallets().stream().filter(w -> w.getTicker().equals("XNO")).findFirst().orElse(null);
    assertNotNull(updatedWallet);
    assertEquals("10000000000000000000000000000", updatedWallet.getRaw());
  }

  @Test
  void testAliasWithDollarInputResolvedUsingCurrentPrice() {
    AliasDto dyn = new AliasDto("GLOBAL", "dyn", "dyns", "XNO", null, ":chart:");
    dyn.setInput("$2 nano");
    lenient()
      .when(aliasesService.analyzeAliases(eq("GLOBAL"), eq("dyn")))
      .thenReturn(dyn);
    CurrencyDto nano = new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30");
    nano.setValue("1.25");
    lenient()
      .when(currenciesService.analyzeCurrencies(any()))
      .thenAnswer(invocation -> {
        String arg = invocation.getArgument(0);
        if (
          arg != null &&
          ("XNO".equalsIgnoreCase(arg) || "nano".equalsIgnoreCase(arg))
        ) {
          return nano;
        }
        return null;
      });

    TransferDto result = transferService.processInputs(
      "gift",
      errorMessageConsumer,
      commandMap,
      "GLOBAL",
      null,
      "1 dyn",
      false
    );

    BigDecimal expected = new BigDecimal("2")
      .divide(new BigDecimal("1.25"), 8, RoundingMode.DOWN)
      .multiply(BigDecimal.TEN.pow(30))
      .stripTrailingZeros();
    WalletDto updatedWallet = result
      .getWallets()
      .stream()
      .filter(w -> w.getTicker().equals("XNO"))
      .findFirst()
      .orElse(null);
    assertNotNull(updatedWallet);
    assertEquals(expected.toPlainString(), updatedWallet.getRaw());
  }

  @Test
  void testValidCreatureWithAmount() {
    lenient().when(creaturesService.analyzeCreatures(eq("shrimps")))
      .thenReturn(new CreatureDto("Shrimp", "Shrimps", "<:shrimp:>", "XNO"));
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));

    TransferDto result =
      transferService.processInputs(
        "gift",
        errorMessageConsumer,
        commandMap,
        "GLOBAL",
        null,
        "2 shrimps",
        false
      );

    ItemDto updatedItem =
      result.getItems().stream().filter(i -> i.getName().equalsIgnoreCase("SHRIMP")).findFirst().orElse(null);
    assertNotNull(updatedItem);
    assertEquals(2, updatedItem.getQuantity());
  }

  @Test
  void testValidAliasWithoutAmount() {
    lenient().when(aliasesService.analyzeAliases(eq("GLOBAL"), eq("small")))
      .thenReturn(
        new AliasDto("GLOBAL", "small", "smalls", "XNO", "1000000000000000000000000000", ":mag:")
      );
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));

    TransferDto result =
      transferService.processInputs(
        "gift",
        errorMessageConsumer,
        commandMap,
        "GLOBAL",
        null,
        "small",
        false
      );

    WalletDto updatedWallet =
      result.getWallets().stream().filter(w -> w.getTicker().equals("XNO")).findFirst().orElse(null);
    assertNotNull(updatedWallet);
    assertEquals("1000000000000000000000000000", updatedWallet.getRaw());
  }

  @Test
  void testValidCreatureAndCurrencyWithAmount() {
    lenient().when(creaturesService.analyzeCreatures(eq("fishes")))
      .thenReturn(new CreatureDto("fish", "fishes", "<:fish:>", "XNO"));
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    lenient().when(currenciesService.analyzeCurrencies(eq("ban")))
      .thenReturn(new CurrencyDto("BAN", "Banano", true, "<:ban:>", "29"));

    TransferDto result =
      transferService.processInputs(
        "gift",
        errorMessageConsumer,
        commandMap,
        "GLOBAL",
        null,
        "2 fishes + .5 ban",
        false
      );

    ItemDto updatedItem =
      result.getItems().stream().filter(i -> i.getName().equals("FISH")).findFirst().orElse(null);
    WalletDto updatedWallet =
      result.getWallets().stream().filter(w -> w.getTicker().equals("BAN")).findFirst().orElse(null);
    assertNotNull(updatedItem);
    assertNotNull(updatedWallet);
    assertEquals(2, updatedItem.getQuantity());
    assertEquals("50000000000000000000000000000", updatedWallet.getRaw());
  }

  @Test
  void processInputsReturnsErrorWhenAddingToAllValue() {
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    lenient().when(userWalletsService.getUsersWallets(eq("user1")))
      .thenReturn(List.of(walletEntity("user1", "XNO", "100")));

    final String[] captured = new String[1];
    Consumer<String> captureError = msg -> captured[0] = msg;

    transferService.processInputs(
      "gift",
      captureError,
      commandMap,
      "GLOBAL",
      "user1",
      "all XNO + all XNO",
      false
    );

    assertNotNull(captured[0]);
    assertTrue(
      captured[0].contains("Cannot add to an \"ALL\" value"),
      "Expected ALL addition error: " + captured[0]
    );
  }

  @Test
  void processInputsReturnsErrorWhenExceedingMaximumTransferParts() {
    final String[] captured = new String[1];
    Consumer<String> captureError = msg -> captured[0] = msg;
    String tooManyParts =
      "1 nano + 2 nano + 3 nano + 4 nano + 5 nano + 6 nano";

    transferService.processInputs(
      "gift",
      captureError,
      commandMap,
      "GLOBAL",
      null,
      tooManyParts,
      false
    );

    assertNotNull(captured[0]);
    assertTrue(
      captured[0].contains("maximum number of concatenated transfers"),
      "Expected max parts error: " + captured[0]
    );
    assertTrue(
      captured[0].contains(String.valueOf(com.nanobot.nanobotbackend.util.Constants.maximumTransferParts)),
      "Expected max value in message: " + captured[0]
    );
  }

  @Test
  void testInvalidInput() {
    final String[] captured = new String[1];
    Consumer<String> captureError = msg -> captured[0] = msg;

    when(aliasesService.analyzeAliases(any(), any())).thenReturn(null);
    when(creaturesService.analyzeCreatures(any())).thenReturn(null);
    when(currenciesService.analyzeCurrencies(any())).thenReturn(null);

    transferService.processInputs(
      "gift",
      captureError,
      commandMap,
      "GLOBAL",
      null,
      "invalidInput",
      false
    );

    assertNotNull(captured[0]);
    assertTrue(captured[0].contains("Invalid Input: invalidInput"));
    assertTrue(captured[0].contains("Try inputs like"));
  }

  @Test
  void testAliasWithComplexAmount() {
    lenient().when(aliasesService.analyzeAliases(eq("GLOBAL"), eq("visionary")))
      .thenReturn(
        new AliasDto(
          "GLOBAL",
          "visionary",
          "visionaries",
          "XNO",
          "100000000000000000000000000",
          "<:visionary:>"
        )
      );
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));

    TransferDto result =
      transferService.processInputs(
        "gift",
        errorMessageConsumer,
        commandMap,
        "GLOBAL",
        null,
        "2 visionary",
        false
      );

    WalletDto updatedWallet =
      result.getWallets().stream().filter(w -> w.getTicker().equals("XNO")).findFirst().orElse(null);
    assertNotNull(updatedWallet);
    assertEquals("200000000000000000000000000", updatedWallet.getRaw());
  }

  @Test
  void testMultipleValidInputs() {
    lenient().when(aliasesService.analyzeAliases(eq("GLOBAL"), eq("puff")))
      .thenReturn(
        new AliasDto("GLOBAL", "puff", "puffs", "BAN", "1000000000000000000000000000", "🌿")
      );
    lenient().when(creaturesService.analyzeCreatures(eq("crabs")))
      .thenReturn(new CreatureDto("crab", "crabs", "<:crab:>", "XNO"));
    lenient().when(currenciesService.analyzeCurrencies(eq("BAN")))
      .thenReturn(new CurrencyDto("BAN", "Banano", true, "<:ban:>", "29"));
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));

    TransferDto result =
      transferService.processInputs(
        "gift",
        errorMessageConsumer,
        commandMap,
        "GLOBAL",
        null,
        "1 puff + 2 crabs",
        false
      );

    ItemDto updatedItem =
      result.getItems().stream().filter(i -> i.getName().equalsIgnoreCase("CRAB")).findFirst().orElse(null);
    WalletDto updatedWallet =
      result.getWallets().stream().filter(w -> w.getTicker().equals("BAN")).findFirst().orElse(null);
    assertNotNull(updatedItem);
    assertNotNull(updatedWallet);
    assertEquals(2, updatedItem.getQuantity());
    assertEquals("1000000000000000000000000000", updatedWallet.getRaw());
  }

  @Test
  void transferReturnsErrorWhenNoReceiversProvided() {
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(new WalletDto("XNO", "10", true)), List.of()),
      null,
      Set.of(walletEntity("sender", "XNO", "10")),
      Set.of(itemsEntity("sender")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of(),
      response,
      true
    );

    assertEquals("Transfer requires at least one receiver.", result.getErrorMessage());
  }

  @Test
  void transferDeduplicatesReceiversAndConservesItemBalances() {
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(), List.of(new ItemDto("CRAB", 3, true))),
      null,
      Set.of(walletEntity("sender", "XNO", "100")),
      Set.of(itemsEntity("sender", "CRAB", 3), itemsEntity("A"), itemsEntity("B")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of("A", "A", "B", "B"),
      response,
      true
    );

    assertNull(result.getErrorMessage());
    assertEquals(1, getItemQuantity(result, "sender", "CRAB"));
    assertEquals(1, getItemQuantity(result, "A", "CRAB"));
    assertEquals(1, getItemQuantity(result, "B", "CRAB"));
    assertTrue(result.getCompletedPrimaryTransfers().isPresent());
    assertEquals(2, result.getCompletedPrimaryTransfers().get().size());
  }

  @Test
  void transferReturnsIndivisibleErrorForRequiredWalletSplit() {
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(new WalletDto("XNO", "1", true)), List.of()),
      null,
      Set.of(walletEntity("sender", "XNO", "1"), walletEntity("A"), walletEntity("B")),
      Set.of(itemsEntity("sender"), itemsEntity("A"), itemsEntity("B")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of("A", "B"),
      response,
      true
    );

    assertEquals(
      "Transfer of XNO is indivisible amongst receivers!",
      result.getErrorMessage()
    );
  }

  @Test
  void transferReturnsIndivisibleErrorForRequiredItemSplit() {
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(), List.of(new ItemDto("SHRIMP", 1, true))),
      null,
      Set.of(walletEntity("sender"), walletEntity("A"), walletEntity("B")),
      Set.of(itemsEntity("sender", "SHRIMP", 1), itemsEntity("A"), itemsEntity("B")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of("A", "B"),
      response,
      true
    );

    assertEquals(
      "Transfer of SHRIMP is indivisible amongst receivers!",
      result.getErrorMessage()
    );
  }

  @Test
  void processInputsReturnsErrorWhenAllWalletRequestedButUserHasNoWallets() {
    lenient().when(currenciesService.analyzeCurrencies(eq("all"))).thenReturn(null);
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    when(userWalletsService.getUsersWallets(eq("user1"))).thenReturn(List.of());

    final String[] captured = new String[1];
    Consumer<String> captureError = msg -> captured[0] = msg;

    transferService.processInputs(
      "gift",
      captureError,
      commandMap,
      "GLOBAL",
      "user1",
      "all XNO",
      false
    );

    assertNotNull(captured[0]);
    assertTrue(
      captured[0].contains("cannot be fulfilled"),
      "Expected balance error: " + captured[0]
    );
    assertTrue(
      captured[0].contains("wallet"),
      "Expected wallet mention: " + captured[0]
    );
  }

  @Test
  void processInputsReturnsErrorWhenAllCreatureRequestedButUserHasNoItems() {
    lenient().when(creaturesService.analyzeCreatures(eq("all"))).thenReturn(null);
    lenient().when(creaturesService.analyzeCreatures(eq("shrimps")))
      .thenReturn(new CreatureDto("Shrimp", "Shrimps", "<:shrimp:>", "XNO"));
    lenient().when(currenciesService.analyzeCurrencies(eq("XNO")))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    when(userItemsService.getUsersItems(eq("user1"))).thenReturn(List.of());

    final String[] captured = new String[1];
    Consumer<String> captureError = msg -> captured[0] = msg;

    transferService.processInputs(
      "sell",
      captureError,
      commandMap,
      "GLOBAL",
      "user1",
      "all shrimps",
      false
    );

    assertNotNull(captured[0]);
    assertTrue(
      captured[0].contains("cannot be fulfilled"),
      "Expected balance error: " + captured[0]
    );
    assertTrue(
      captured[0].contains("inventory"),
      "Expected inventory mention: " + captured[0]
    );
  }

  @Test
  void transferFromSystemUserZeroMintsToReceivers() {
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(new WalletDto("XNO", "100", true)), List.of()),
      null,
      Set.of(walletEntity("A"), walletEntity("B")),
      Set.of(itemsEntity("A"), itemsEntity("B")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "0",
      List.of("A", "B"),
      response,
      true
    );

    assertNull(result.getErrorMessage());
    assertEquals("50", getWalletRaw(result, "A", "XNO"));
    assertEquals("50", getWalletRaw(result, "B", "XNO"));
  }

  private String getWalletRaw(
    TransferResponseDto response,
    String userId,
    String ticker
  ) {
    return response
      .getUserWalletQuantities()
      .stream()
      .filter(e -> e.getUserId().equals(userId))
      .findFirst()
      .orElseThrow()
      .getWallets()
      .stream()
      .filter(w -> w.getTicker().equals(ticker))
      .findFirst()
      .orElseThrow()
      .getRaw();
  }

  @Test
  void transferToSystemUserZeroBurnsFromSender() {
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(new WalletDto("XNO", "100", true)), List.of()),
      null,
      Set.of(walletEntity("sender", "XNO", "100"), walletEntity("0")),
      Set.of(itemsEntity("sender"), itemsEntity("0")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of("0"),
      response,
      true
    );

    assertNull(result.getErrorMessage());
    assertEquals("0", getWalletRaw(result, "sender", "XNO"));
  }

  @Test
  void transferReturnsBalanceErrorWhenRequiredWalletMissing() {
    when(currenciesService.analyzeCurrencies("XNO"))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    when(currenciesService.getCurrencyDecimalValue(anyString(), anyInt()))
      .thenAnswer(invocation -> invocation.getArgument(0));

    TransferResponseDto response = baseTransferResponse(
      new TransferDto(List.of(new WalletDto("XNO", "5", true)), List.of()),
      null,
      Set.of(walletEntity("sender"), walletEntity("A")),
      Set.of(itemsEntity("sender"), itemsEntity("A")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of("A"),
      response,
      true
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("cannot be fulfilled"));
    assertTrue(result.getErrorMessage().contains("balance of **0 XNO**"));
  }

  private TransferResponseDto baseTransferResponse(
    TransferDto primaryTransfer,
    TransferDto secondaryTransfer,
    Set<UserWalletsEntity> userWallets,
    Set<UserItemsEntity> userItems,
    Set<com.nanobot.nanobotbackend.entity.GuildWalletsEntity> guildWallets
  ) {
    TransferResponseDto response = new TransferResponseDto();
    response.setPrimaryTransfer(primaryTransfer);
    response.setSecondaryTransfer(secondaryTransfer);
    response.setUserWalletQuantities(userWallets);
    response.setUserItemQuantities(userItems);
    response.setGuildWalletQuantities(guildWallets);
    response.setCommands(
      List.of(
        new CommandDto("wallet", "wallet-cmd"),
        new CommandDto("inventory", "inventory-cmd")
      )
    );
    return response;
  }

  private UserWalletsEntity walletEntity(String userId) {
    UserWalletsEntity entity = new UserWalletsEntity(userId);
    entity.setWallets(new ArrayList<>());
    return entity;
  }

  private UserWalletsEntity walletEntity(String userId, String ticker, String raw) {
    UserWalletsEntity entity = walletEntity(userId);
    entity.getWallets().add(new WalletDto(ticker, raw));
    return entity;
  }

  private UserItemsEntity itemsEntity(String userId) {
    UserItemsEntity entity = new UserItemsEntity(userId);
    entity.setItems(new ArrayList<>());
    return entity;
  }

  private UserItemsEntity itemsEntity(String userId, String name, int quantity) {
    UserItemsEntity entity = itemsEntity(userId);
    entity.getItems().add(new ItemDto(name, quantity));
    return entity;
  }

  private int getItemQuantity(
    TransferResponseDto response,
    String userId,
    String itemName
  ) {
    return response
      .getUserItemQuantities()
      .stream()
      .filter(entity -> entity.getUserId().equals(userId))
      .findFirst()
      .orElseThrow()
      .getItems()
      .stream()
      .filter(item -> item.getName().equalsIgnoreCase(itemName))
      .findFirst()
      .orElseThrow()
      .getQuantity();
  }

  @Test
  void transferCreditsGuildWalletWhenReceiverIsBot() {
    String botUserId = System.getenv("BOT_USER_ID");
    if (botUserId == null) {
      botUserId = "test-bot-user-id";
    }
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(
        List.of(new WalletDto("XNO", "100", true)),
        List.of()
      ),
      null,
      Set.of(walletEntity("sender", "XNO", "100"), walletEntity(botUserId)),
      Set.of(itemsEntity("sender"), itemsEntity(botUserId)),
      new java.util.HashSet<>()
    );
    response.getUserWalletQuantities().stream()
      .filter(e -> e.getUserId().equals("sender"))
      .findFirst()
      .orElseThrow()
      .getWallets()
      .add(new WalletDto("XNO", "100"));

    TransferResponseDto result = transferService.transfer(
      "guild1",
      "sender",
      List.of(botUserId),
      response,
      true
    );

    assertNull(result.getErrorMessage());
    assertNotNull(result.getGuildWalletQuantities());
    assertFalse(result.getGuildWalletQuantities().isEmpty());
    GuildWalletsEntity guildWallet = result.getGuildWalletQuantities().iterator().next();
    WalletDto xnoWallet = guildWallet.getWallets().stream()
      .filter(w -> "XNO".equals(w.getTicker()))
      .findFirst()
      .orElse(null);
    assertNotNull(xnoWallet);
    assertEquals("100", xnoWallet.getRaw());
  }

  @Test
  void transferReturnsErrorWhenBotSenderHasInsufficientGuildReserve() {
    when(currenciesService.analyzeCurrencies("XNO"))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    when(currenciesService.getCurrencyDecimalValue(anyString(), anyInt()))
      .thenAnswer(invocation -> invocation.getArgument(0));

    String botUserId = System.getenv("BOT_USER_ID");
    if (botUserId == null) {
      botUserId = "test-bot-user-id";
    }
    GuildWalletsEntity guildWallet = new GuildWalletsEntity("guild1");
    guildWallet.setWallets(List.of(new WalletDto("XNO", "10")));
    TransferResponseDto response = baseTransferResponse(
      new TransferDto(
        List.of(new WalletDto("XNO", "100", true)),
        List.of()
      ),
      null,
      Set.of(walletEntity("receiver", "XNO", "0")),
      Set.of(itemsEntity("receiver")),
      Set.of(guildWallet)
    );

    TransferResponseDto result = transferService.transfer(
      "guild1",
      botUserId,
      List.of("receiver"),
      response,
      true
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("cannot be fulfilled"),
      "Expected balance error: " + result.getErrorMessage()
    );
  }

  @Test
  void transferReturnsItemBalanceErrorWhenRequiredItemMissing() {
    lenient().when(currenciesService.analyzeCurrencies("XNO"))
      .thenReturn(new CurrencyDto("XNO", "Nano", true, "<:xno:>", "30"));
    lenient().when(currenciesService.getCurrencyDecimalValue(anyString(), anyInt()))
      .thenAnswer(invocation -> invocation.getArgument(0));

    TransferResponseDto response = baseTransferResponse(
      new TransferDto(
        List.of(),
        List.of(new ItemDto("SHRIMP", 5, true))
      ),
      null,
      Set.of(walletEntity("sender"), walletEntity("A")),
      Set.of(itemsEntity("sender"), itemsEntity("A")),
      null
    );

    TransferResponseDto result = transferService.transfer(
      "guild",
      "sender",
      List.of("A"),
      response,
      true
    );

    assertNotNull(result.getErrorMessage());
    assertTrue(result.getErrorMessage().contains("cannot be fulfilled"));
    assertTrue(result.getErrorMessage().contains("inventory"));
  }
}
