package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.service.ActiveServices;
import com.nanobot.nanobotbackend.service.AliasesServices;
import com.nanobot.nanobotbackend.service.AuditServices;
import com.nanobot.nanobotbackend.service.ConfigServices;
import com.nanobot.nanobotbackend.service.CoreServices;
import com.nanobot.nanobotbackend.service.CreaturesServices;
import com.nanobot.nanobotbackend.service.CurrenciesServices;
import com.nanobot.nanobotbackend.service.DropService;
import com.nanobot.nanobotbackend.service.FishServices;
import com.nanobot.nanobotbackend.service.FishingReminderService;
import com.nanobot.nanobotbackend.service.InventoryServices;
import com.nanobot.nanobotbackend.service.LeaderboardsServices;
import com.nanobot.nanobotbackend.service.PickupServices;
import com.nanobot.nanobotbackend.service.SendServices;
import com.nanobot.nanobotbackend.service.ServerServices;
import com.nanobot.nanobotbackend.service.TransactionsServices;
import com.nanobot.nanobotbackend.service.TransferServices;
import com.nanobot.nanobotbackend.service.WalletServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RequestControllersSmokeTest {

  private MockMvc mockMvc;

  @Mock
  private ActiveServices activeServices;

  @Mock
  private AliasesServices aliasesServices;

  @Mock
  private AuditServices auditServices;

  @Mock
  private ConfigServices configServices;

  @Mock
  private CoreServices coreServices;

  @Mock
  private CreaturesServices creaturesServices;

  @Mock
  private CurrenciesServices currenciesServices;

  @Mock
  private DropService dropService;

  @Mock
  private FishServices fishServices;

  @Mock
  private FishingReminderService fishingReminderService;

  @Mock
  private InventoryServices inventoryServices;

  @Mock
  private LeaderboardsServices leaderboardsServices;

  @Mock
  private PickupServices pickupServices;

  @Mock
  private SendServices sendServices;

  @Mock
  private ServerServices serverServices;

  @Mock
  private TransactionsServices transactionsServices;

  @Mock
  private TransferServices transferServices;

  @Mock
  private WalletServices walletServices;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(
          new ActiveController(activeServices),
          new AliasesRequestsController(aliasesServices),
          new AuditController(auditServices),
          new ConfigController(configServices),
          new CoreController(coreServices),
          new CreaturesRequestsController(creaturesServices),
          new CurrenciesRequestsController(currenciesServices),
          new DropController(dropService),
          new FishController(fishServices),
          new FishingReminderController(fishingReminderService),
          new HealthController(),
          new InventoryController(inventoryServices),
          new LeaderboardsRequestsController(leaderboardsServices),
          new PickupController(pickupServices),
          new SendController(sendServices),
          new ServerController(serverServices),
          new TransactionsRequestsController(transactionsServices),
          new TransferController(transferServices),
          new WalletController(walletServices)
        )
        .build();
  }

  @Test
  void healthShouldReturnOk() throws Exception {
    mockMvc
      .perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.containsString("Nanobot Backend is Healthy")));
  }

  @Test
  void activeShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/active").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(activeServices).active(any());
  }

  @Test
  void aliasesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/aliases").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(aliasesServices).aliases(any());
  }

  @Test
  void auditShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/audit").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(auditServices).audit(any());
  }

  @Test
  void configShouldReturnAccepted() throws Exception {
    mockMvc
      .perform(put("/requests/config").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isAccepted());
    verify(configServices).config(any());
  }

  @Test
  void coreCuteShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/cute").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(coreServices).cute(any());
  }

  @Test
  void coreBonusesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/bonuses").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(coreServices).bonuses(any());
  }

  @Test
  void coreRolesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/roles").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(coreServices).roles(any());
  }

  @Test
  void coreRulesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/rules").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(coreServices).rules(any());
  }

  @Test
  void creaturesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/creatures").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(creaturesServices).creatures(any());
  }

  @Test
  void currenciesHelpShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/help").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(currenciesServices).help(any());
  }

  @Test
  void currenciesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/currencies").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(currenciesServices).currencies(any());
  }

  @Test
  void currenciesReceiveShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/receive").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(currenciesServices).receive(any());
  }

  @Test
  void dropUpdateShouldReturnAccepted() throws Exception {
    mockMvc
      .perform(post("/drop/update").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isAccepted());
    verify(dropService).dropUpdate(any());
  }

  @Test
  void fishShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/fish").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(fishServices).fish(any());
  }

  @Test
  void remindersShouldReturnOk() throws Exception {
    mockMvc.perform(get("/reminders")).andExpect(status().isOk());
    verify(fishingReminderService).getFishingReminder();
  }

  @Test
  void inventoryShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/inventory").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(inventoryServices).inventory(any());
  }

  @Test
  void leaderboardsShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/leaderboards").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(leaderboardsServices).leaderboards(any());
  }

  @Test
  void pickupShouldReturnAccepted() throws Exception {
    mockMvc
      .perform(post("/requests/pickup").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isAccepted());
    verify(pickupServices).pickup(any());
  }

  @Test
  void sendUpdateShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/update").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(sendServices).update(any());
  }

  @Test
  void sendShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/send").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(sendServices).send(any());
  }

  @Test
  void serverShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/server").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(serverServices).configurations(any());
  }

  @Test
  void reservesShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/reserves").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(serverServices).reserves(any());
  }

  @Test
  void transactionsShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/transactions").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(transactionsServices).transactions(any());
  }

  @Test
  void transferDropShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/drop").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(transferServices).drop(any());
  }

  @Test
  void transferGiftShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/gift").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(transferServices).gift(any());
  }

  @Test
  void transferMergeShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/merge").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(transferServices).merge(any());
  }

  @Test
  void transferRainShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/rain").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(transferServices).rain(any());
  }

  @Test
  void transferSellShouldReturnOk() throws Exception {
    mockMvc
      .perform(put("/requests/sell").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(transferServices).sell(any());
  }

  @Test
  void walletShouldReturnOk() throws Exception {
    mockMvc
      .perform(post("/requests/wallet").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());
    verify(walletServices).wallet(any());
  }
}
