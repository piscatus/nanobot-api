package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.repository.GuildWalletsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class GuildWalletsServiceImplTest {

  @Mock
  private GuildWalletsRepository guildWalletsRepository;

  private GuildWalletsServiceImpl guildWalletsService;

  @BeforeEach
  void setUp() {
    guildWalletsService = new GuildWalletsServiceImpl(guildWalletsRepository);
  }

  @Test
  void setGuildWalletsShouldNotCallConsumerWhenGuildIdInvalid() {
    AtomicReference<List<WalletDto>> captured = new AtomicReference<>();
    guildWalletsService.setGuildWallets(null, captured::set);
    assertNull(captured.get());

    guildWalletsService.setGuildWallets("", captured::set);
    assertNull(captured.get());
  }

  @Test
  void setGuildWalletsShouldAcceptConsumerWhenSingleGuildFound() {
    WalletDto wallet = new WalletDto();
    wallet.setTicker("NANO");
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    entity.setWallets(List.of(wallet));
    when(guildWalletsRepository.findByGuildId("g1")).thenReturn(List.of(entity));

    AtomicReference<List<WalletDto>> captured = new AtomicReference<>();
    guildWalletsService.setGuildWallets("g1", captured::set);

    assertEquals(1, captured.get().size());
    assertEquals("NANO", captured.get().get(0).getTicker());
    verify(guildWalletsRepository).findByGuildId("g1");
  }

  @Test
  void setAllGuildsWalletsShouldAcceptConsumerWithAllGuilds() {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    when(guildWalletsRepository.findAll()).thenReturn(List.of(entity));

    AtomicReference<List<GuildWalletsDto>> captured = new AtomicReference<>();
    guildWalletsService.setAllGuildsWallets(captured::set);

    assertEquals(1, captured.get().size());
    assertEquals("g1", captured.get().get(0).getGuildId());
    verify(guildWalletsRepository).findAll();
  }

  @Test
  void createGuildWalletsShouldReturnEntityWhenSuccess() {
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setGuildId("g1");
    when(guildWalletsRepository.insert(any(GuildWalletsEntity.class))).thenAnswer((Answer<GuildWalletsEntity>) inv -> {
      GuildWalletsEntity e = inv.getArgument(0);
      e.setId("gw-new");
      return e;
    });

    Optional<GuildWalletsEntity> result = guildWalletsService.createGuildWallets(dto);

    assertTrue(result.isPresent());
    assertEquals("gw-new", result.get().getId());
    verify(guildWalletsRepository).insert(any(GuildWalletsEntity.class));
  }

  @Test
  void createGuildWalletsShouldReturnEmptyWhenDuplicateKey() {
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setGuildId("g1");
    when(guildWalletsRepository.insert(any(GuildWalletsEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<GuildWalletsEntity> result = guildWalletsService.createGuildWallets(dto);

    assertFalse(result.isPresent());
    verify(guildWalletsRepository).insert(any(GuildWalletsEntity.class));
  }

  @Test
  void getGuildsWalletsShouldReturnAllWhenGuildIdNull() {
    List<GuildWalletsEntity> entities = List.of(new GuildWalletsEntity("g1"));
    when(guildWalletsRepository.findAll()).thenReturn(entities);

    List<GuildWalletsEntity> result = guildWalletsService.getGuildsWallets(null);

    assertEquals(1, result.size());
    verify(guildWalletsRepository).findAll();
  }

  @Test
  void getGuildsWalletsShouldReturnByGuildId() {
    List<GuildWalletsEntity> entities = List.of(new GuildWalletsEntity("g1"));
    when(guildWalletsRepository.findByGuildId("g1")).thenReturn(entities);

    List<GuildWalletsEntity> result = guildWalletsService.getGuildsWallets("g1");

    assertEquals(1, result.size());
    verify(guildWalletsRepository).findByGuildId("g1");
  }

  @Test
  void getGuildWalletsByIdShouldReturnEmptyWhenIdNull() {
    Optional<GuildWalletsEntity> result = guildWalletsService.getGuildWalletsById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getGuildWalletsByIdShouldReturnEntityWhenFound() {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    when(guildWalletsRepository.findById("gw1")).thenReturn(Optional.of(entity));

    Optional<GuildWalletsEntity> result = guildWalletsService.getGuildWalletsById("gw1");

    assertTrue(result.isPresent());
    assertEquals("gw1", result.get().getId());
    verify(guildWalletsRepository).findById("gw1");
  }

  @Test
  void getGuildWalletsByGuildIdShouldReturnEmptyWhenGuildIdNull() {
    Optional<GuildWalletsEntity> result =
      guildWalletsService.getGuildWalletsByGuildId(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getGuildWalletsByGuildIdShouldReturnEntityWhenFound() {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    when(guildWalletsRepository.findByGuildId("g1")).thenReturn(List.of(entity));

    Optional<GuildWalletsEntity> result =
      guildWalletsService.getGuildWalletsByGuildId("g1");

    assertTrue(result.isPresent());
    assertEquals("g1", result.get().getGuildId());
    verify(guildWalletsRepository).findByGuildId("g1");
  }

  @Test
  void updateGuildWalletsShouldReturnEmptyWhenIdNull() {
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setGuildId("g1");

    Optional<GuildWalletsEntity> result =
      guildWalletsService.updateGuildWallets(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateGuildWalletsShouldReturnUpdatedEntityWhenFound() {
    GuildWalletsEntity existing = new GuildWalletsEntity("g1");
    existing.setId("gw1");
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setGuildId("g2");
    dto.setWallets(new ArrayList<>());
    when(guildWalletsRepository.findById("gw1")).thenReturn(Optional.of(existing));
    when(guildWalletsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<GuildWalletsEntity> result =
      guildWalletsService.updateGuildWallets("gw1", dto);

    assertTrue(result.isPresent());
    assertEquals("g2", result.get().getGuildId());
    verify(guildWalletsRepository).findById("gw1");
    verify(guildWalletsRepository).save(any());
  }

  @Test
  void deleteGuildWalletsShouldReturnEmptyWhenIdNull() {
    Optional<GuildWalletsEntity> result = guildWalletsService.deleteGuildWallets(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteGuildWalletsShouldReturnUpdatedEntityWhenFound() {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    WalletDto wallet = new WalletDto();
    wallet.setRaw("100");
    entity.setWallets(new ArrayList<>(List.of(wallet)));
    when(guildWalletsRepository.findById("gw1")).thenReturn(Optional.of(entity));
    when(guildWalletsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<GuildWalletsEntity> result = guildWalletsService.deleteGuildWallets("gw1");

    assertTrue(result.isPresent());
    assertEquals("0", result.get().getWallets().get(0).getRaw());
    verify(guildWalletsRepository).findById("gw1");
    verify(guildWalletsRepository).save(any());
  }

  @Test
  void saveWalletShouldCallRepository() {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    when(guildWalletsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    guildWalletsService.saveWallet(entity);

    verify(guildWalletsRepository).save(entity);
  }
}
