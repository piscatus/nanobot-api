package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.entity.VerificationEntity;
import com.nanobot.nanobotbackend.service.ActivitiesService;
import com.nanobot.nanobotbackend.service.AliasesService;
import com.nanobot.nanobotbackend.service.AnglersService;
import com.nanobot.nanobotbackend.service.CommandsService;
import com.nanobot.nanobotbackend.service.CreaturesService;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.service.DropsService;
import com.nanobot.nanobotbackend.service.EmojisService;
import com.nanobot.nanobotbackend.service.GuildConfigurationsService;
import com.nanobot.nanobotbackend.service.GuildWalletsService;
import com.nanobot.nanobotbackend.service.LeaderboardsService;
import com.nanobot.nanobotbackend.service.MessagesService;
import com.nanobot.nanobotbackend.service.PickupsService;
import com.nanobot.nanobotbackend.service.ProfanitiesService;
import com.nanobot.nanobotbackend.service.QueuesService;
import com.nanobot.nanobotbackend.service.TransactionsService;
import com.nanobot.nanobotbackend.service.UserDetailsService;
import com.nanobot.nanobotbackend.service.UserItemsService;
import com.nanobot.nanobotbackend.service.UserWalletsService;
import com.nanobot.nanobotbackend.service.VerificationsService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CrudControllersSmokeTest {

  private MockMvc mockMvc;

  @Mock
  private ActivitiesService activitiesService;

  @Mock
  private AliasesService aliasesService;

  @Mock
  private AnglersService anglersService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private DropsService dropsService;

  @Mock
  private EmojisService emojisService;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private GuildWalletsService guildWalletsService;

  @Mock
  private LeaderboardsService leaderboardsService;

  @Mock
  private MessagesService messagesService;

  @Mock
  private PickupsService pickupsService;

  @Mock
  private ProfanitiesService profanitiesService;

  @Mock
  private QueuesService queuesService;

  @Mock
  private TransactionsService transactionsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private UserItemsService userItemsService;

  @Mock
  private UserWalletsService userWalletsService;

  @Mock
  private VerificationsService verificationsService;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(
          new ActivitiesController(activitiesService),
          new AliasesController(aliasesService),
          new AnglersController(anglersService),
          new CommandsController(commandsService),
          new CreaturesController(creaturesService),
          new CurrenciesController(currenciesService),
          new DropsController(dropsService),
          new EmojisController(emojisService),
          new GuildConfigurationsController(guildConfigurationsService),
          new GuildWalletsController(guildWalletsService),
          new LeaderboardsController(leaderboardsService),
          new MessagesController(messagesService),
          new PickupsController(pickupsService),
          new ProfanitiesController(profanitiesService),
          new QueuesController(queuesService),
          new TransactionsController(transactionsService),
          new UserDetailsController(userDetailsService),
          new UserItemsController(userItemsService),
          new UserWalletsController(userWalletsService),
          new VerificationsController(verificationsService, emojisService)
        )
        .build();
  }

  @Test
  void activitiesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/activities")).andExpect(status().isOk());
    verify(activitiesService).getActivities(isNull(), isNull(), isNull());
  }

  @Test
  void aliasesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/aliases")).andExpect(status().isOk());
    verify(aliasesService).getAliases(isNull(), isNull());
  }

  @Test
  void anglersGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/anglers")).andExpect(status().isOk());
    verify(anglersService).getAnglers(isNull(), isNull());
  }

  @Test
  void commandsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/commands")).andExpect(status().isOk());
    verify(commandsService).getCommands(isNull());
  }

  @Test
  void creaturesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/creatures")).andExpect(status().isOk());
    verify(creaturesService).getCreatures();
  }

  @Test
  void currenciesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/currencies")).andExpect(status().isOk());
    verify(currenciesService).getCurrencies(isNull());
  }

  @Test
  void dropsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/drops")).andExpect(status().isOk());
    verify(dropsService).getDrops(isNull());
  }

  @Test
  void emojisGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/emojis")).andExpect(status().isOk());
    verify(emojisService).getEmojis(isNull(), isNull());
  }

  @Test
  void guildConfigurationsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/guildConfigurations")).andExpect(status().isOk());
    verify(guildConfigurationsService).getGuildConfigurations(isNull());
  }

  @Test
  void guildWalletsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/guildWallets")).andExpect(status().isOk());
    verify(guildWalletsService).getGuildsWallets(isNull());
  }

  @Test
  void leaderboardsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/leaderboards")).andExpect(status().isOk());
    verify(leaderboardsService).getLeaderboards(isNull(), isNull());
  }

  @Test
  void messagesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/messages")).andExpect(status().isOk());
    verify(messagesService).getMessages();
  }

  @Test
  void pickupsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/pickups")).andExpect(status().isOk());
    verify(pickupsService).getPickups(isNull(), isNull());
  }

  @Test
  void profanitiesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/profanities")).andExpect(status().isOk());
    verify(profanitiesService).getProfanities(isNull());
  }

  @Test
  void queuesGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/queues")).andExpect(status().isOk());
    verify(queuesService).getQueues();
  }

  @Test
  void transactionsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/transactions")).andExpect(status().isOk());
    verify(transactionsService).getTransactions(isNull());
  }

  @Test
  void userDetailsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/userDetails")).andExpect(status().isOk());
    verify(userDetailsService).getUsersDetails(isNull());
  }

  @Test
  void userItemsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/userItems")).andExpect(status().isOk());
    verify(userItemsService).getUsersItems(isNull());
  }

  @Test
  void userWalletsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/userWallets")).andExpect(status().isOk());
    verify(userWalletsService).getUsersWallets(isNull());
  }

  @Test
  void verificationsGetShouldReturnOk() throws Exception {
    mockMvc.perform(get("/verifications")).andExpect(status().isOk());
    verify(verificationsService).getVerifications(isNull());
  }

  @Test
  void verificationsCheckShouldReturnOkWhenRecentVerificationExists() throws Exception {
    VerificationEntity verification = new VerificationEntity();
    verification.setTimestamp(new Date());
    when(verificationsService.getVerificationByUserId("u1")).thenReturn(
      Optional.of(verification)
    );

    mockMvc
      .perform(get("/verifications/userId/u1/check"))
      .andExpect(status().isOk());

    verify(verificationsService).getVerificationByUserId("u1");
    verify(emojisService, never()).getEmojis(isNull(), isNull());
  }

  @Test
  void verificationsCheckShouldReturnUnauthorizedWhenMissingVerification() throws Exception {
    EmojiEntity b = new EmojiEntity();
    b.setCategory("b");
    EmojiEntity a = new EmojiEntity();
    a.setCategory("a");

    when(verificationsService.getVerificationByUserId("u1")).thenReturn(
      Optional.empty()
    );
    when(emojisService.getEmojis(isNull(), isNull())).thenReturn(
      new ArrayList<>(List.of(b, a))
    );

    mockMvc
      .perform(get("/verifications/userId/u1/check"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$[0].category").value("a"))
      .andExpect(jsonPath("$[1].category").value("b"));

    verify(verificationsService).getVerificationByUserId("u1");
    verify(emojisService).getEmojis(isNull(), isNull());
  }
}
