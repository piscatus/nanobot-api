package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.*;
import com.nanobot.nanobotbackend.entity.*;
import com.nanobot.nanobotbackend.util.Constants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FishServicesImplTest {

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private CommandsService commandsService;

  @Mock
  private AnglersService anglersService;

  @Mock
  private CurrenciesService currenciesService;

  @Mock
  private GuildWalletsService guildWalletsService;

  @Mock
  private LeaderboardsService leaderboardsService;

  @Mock
  private CreaturesService creaturesService;

  @Mock
  private UserItemsService userItemsService;

  @Mock
  private TransferExecutorService transferExecutorService;

  private CoreServices coreServices;
  private FishServicesImpl fishServices;

  @BeforeEach
  void setUp() {
    coreServices =
      new CoreServices(
        commandsService,
        guildConfigurationsService,
        userDetailsService
      );
    fishServices =
      new FishServicesImpl(
        anglersService,
        coreServices,
        currenciesService,
        guildWalletsService,
        leaderboardsService,
        creaturesService,
        userItemsService
      );
    try {
      var field = FishServicesImpl.class.getDeclaredField("transferExecutorService");
      field.setAccessible(true);
      field.set(fishServices, transferExecutorService);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void fishReturnsEarlyWhenGuildConfigurationsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(false);

    TransferResponseDto result = fishServices.fish(request);

    assertNotNull(result);
    assertNull(result.getErrorMessage());
    verify(guildWalletsService, never()).getGuildsWallets(any());
    verify(transferExecutorService, never()).executeTransfer(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void fishReturnsEarlyWhenUserDetailsFail() {
    RequestDto request = minimalRequest();
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenReturn(true);
    when(
      userDetailsService.setUserDetails(
        eq(request.getUserId()),
        any(),
        eq(true)
      )
    )
      .thenReturn(false);

    TransferResponseDto result = fishServices.fish(request);

    assertNotNull(result);
    verify(guildWalletsService, never()).getGuildsWallets(any());
  }

  @Test
  void fishReturnsErrorWhenGuildWalletsEmpty() {
    RequestDto request = minimalRequest();
    setupCoreServicesSuccess(request);
    when(guildWalletsService.getGuildsWallets(request.getGuildId()))
      .thenReturn(Collections.emptyList());

    TransferResponseDto result = fishServices.fish(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("server balance does not meet"),
      "Expected server balance error: " + result.getErrorMessage()
    );
    verify(transferExecutorService, never()).executeTransfer(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void fishReturnsErrorWhenNoAffordableCreatures() {
    RequestDto request = minimalRequest();
    setupCoreServicesSuccess(request);

    GuildWalletsEntity guildWallet = new GuildWalletsEntity("guild1");
    guildWallet.setWallets(
      List.of(new WalletDto("XNO", "1"))
    );
    when(guildWalletsService.getGuildsWallets(request.getGuildId()))
      .thenReturn(List.of(guildWallet));

    CreatureEntity creature = new CreatureEntity();
    creature.setName("Shrimp");
    creature.setTicker("XNO");
    creature.setValue("1000000000000000000000000000");
    creature.setOdds(100);
    when(creaturesService.getCreatures()).thenReturn(List.of(creature));
    doAnswer(
      inv -> {
        inv.getArgument(0, java.util.function.Consumer.class).accept(
          List.of(new CurrencyDto("XNO", "Nano", true, ":xno:", "30"))
        );
        return null;
      }
    )
      .when(currenciesService)
      .setCurrencies(any());

    TransferResponseDto result = fishServices.fish(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("server balance does not meet"),
      "Expected server balance error: " + result.getErrorMessage()
    );
  }

  @Test
  void fishReturnsErrorWhenUserLacksFishingRole() {
    RequestDto request = minimalRequest();
    request.setUserRoles(List.of("other-role"));

    GuildConfigurationsDto guildConfig = new GuildConfigurationsDto();
    guildConfig.setFishingRole("fishing-role-id");
    guildConfig.setFishingBypassRoles(null);
    guildConfig.setFishingError(null);

    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(guildConfig);
          return true;
        }
      );
    when(
      userDetailsService.setUserDetails(
        eq(request.getUserId()),
        any(),
        eq(true)
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(new UserDetailsDto());
          return true;
        }
      );
    when(
      commandsService.setCommands(
        eq(Constants.COMMAND_NAME_FISH),
        eq(request.getUserId()),
        any()
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(2, java.util.function.Consumer.class).accept(
            List.of(
              new CommandDto("fish", "fish-cmd"),
              new CommandDto("gift", "gift-cmd"),
              new CommandDto("reserves", "reserves-cmd"),
              new CommandDto("help", "help-cmd")
            )
          );
          return true;
        }
      );

    lenient().when(guildWalletsService.getGuildsWallets(request.getGuildId()))
      .thenReturn(List.of(guildWalletWithBalance()));
    lenient()
      .doAnswer(
        inv -> {
          inv.getArgument(0, java.util.function.Consumer.class).accept(
            List.of(new CurrencyDto("XNO", "Nano", true, ":xno:", "30"))
          );
          return null;
        }
      )
      .when(currenciesService)
      .setCurrencies(any());
    lenient().when(creaturesService.getCreatures()).thenReturn(affordableCreatures());
    lenient()
      .doAnswer(
        inv -> {
          inv.getArgument(0, java.util.function.Consumer.class).accept(
            List.of(new CreatureDto())
          );
          return null;
        }
      )
      .when(creaturesService)
      .setCreatures(any());
    lenient().when(userItemsService.getUsersItems(request.getUserId())).thenReturn(List.of(new UserItemsEntity(request.getUserId())));
    lenient().when(anglersService.getAnglerByGuildIdAndUserId(request.getGuildId(), request.getUserId()))
      .thenReturn(java.util.Optional.empty());

    TransferResponseDto result = fishServices.fish(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("required role"),
      "Expected role error: " + result.getErrorMessage()
    );
  }

  @Test
  void fishReturnsErrorWhenUserMustWait() {
    RequestDto request = minimalRequest();
    setupCoreServicesSuccess(request);
    lenient().when(guildWalletsService.getGuildsWallets(request.getGuildId()))
      .thenReturn(List.of(guildWalletWithBalance()));
    lenient()
      .doAnswer(
        inv -> {
          inv.getArgument(0, java.util.function.Consumer.class).accept(
            List.of(new CurrencyDto("XNO", "Nano", true, ":xno:", "30"))
          );
          return null;
        }
      )
      .when(currenciesService)
      .setCurrencies(any());
    lenient().when(creaturesService.getCreatures()).thenReturn(affordableCreatures());
    lenient()
      .doAnswer(
        inv -> {
          inv.getArgument(0, java.util.function.Consumer.class).accept(List.of(new CreatureDto()));
          return null;
        }
      )
      .when(creaturesService)
      .setCreatures(any());
    lenient().when(userItemsService.getUsersItems(request.getUserId())).thenReturn(List.of(new UserItemsEntity(request.getUserId())));

    AnglerEntity angler = new AnglerEntity(request.getGuildId(), request.getUserId(), true, new Date());
    lenient().when(anglersService.getAnglerByGuildIdAndUserId(request.getGuildId(), request.getUserId()))
      .thenReturn(java.util.Optional.of(angler));

    TransferResponseDto result = fishServices.fish(request);

    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("must wait") || result.getErrorMessage().contains("crashed"),
      "Expected wait or crash: " + result.getErrorMessage()
    );
  }

  private void setupCoreServicesSuccess(RequestDto request) {
    when(
      guildConfigurationsService.setGuildConfigurations(
        eq(request.getGuildId()),
        any()
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(new GuildConfigurationsDto());
          return true;
        }
      );
    when(
      userDetailsService.setUserDetails(
        eq(request.getUserId()),
        any(),
        eq(true)
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(1, java.util.function.Consumer.class).accept(new UserDetailsDto());
          return true;
        }
      );
    when(
      commandsService.setCommands(
        eq(Constants.COMMAND_NAME_FISH),
        eq(request.getUserId()),
        any()
      )
    )
      .thenAnswer(
        inv -> {
          inv.getArgument(2, java.util.function.Consumer.class).accept(
            List.of(
              new CommandDto("fish", "fish-cmd"),
              new CommandDto("gift", "gift-cmd"),
              new CommandDto("reserves", "reserves-cmd"),
              new CommandDto("help", "help-cmd")
            )
          );
          return true;
        }
      );
  }

  private GuildWalletsEntity guildWalletWithBalance() {
    GuildWalletsEntity entity = new GuildWalletsEntity("guild1");
    entity.setWallets(
      List.of(new WalletDto("XNO", "1000000000000000000000000000000"))
    );
    return entity;
  }

  private List<CreatureEntity> affordableCreatures() {
    CreatureEntity creature = new CreatureEntity();
    creature.setName("Shrimp");
    creature.setTicker("XNO");
    creature.setValue("100000000000000000000000000");
    creature.setOdds(100);
    creature.setCapacity(100);
    return List.of(creature);
  }

  private RequestDto minimalRequest() {
    return new RequestDto(
      null,
      "channel-1",
      false,
      null,
      null,
      false,
      "guild-1",
      null,
      null,
      null,
      null,
      null,
      "user-1",
      new ArrayList<>(),
      null,
      null
    );
  }
}
