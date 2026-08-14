package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import com.nanobot.nanobotbackend.repository.LeaderboardsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class LeaderboardsServiceImplTest {

  @Mock
  private LeaderboardsRepository leaderboardsRepository;

  private LeaderboardsServiceImpl leaderboardsService;

  @BeforeEach
  void setUp() {
    leaderboardsService = new LeaderboardsServiceImpl(leaderboardsRepository);
  }

  @Test
  void createLeaderboardShouldReturnEntityWhenSuccess() {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    when(leaderboardsRepository.insert(any(LeaderboardEntity.class))).thenAnswer((Answer<LeaderboardEntity>) inv -> {
      LeaderboardEntity e = inv.getArgument(0);
      e.setId("lb-new");
      return e;
    });

    Optional<LeaderboardEntity> result = leaderboardsService.createLeaderboard(dto);

    assertTrue(result.isPresent());
    assertEquals("lb-new", result.get().getId());
    verify(leaderboardsRepository).insert(any(LeaderboardEntity.class));
  }

  @Test
  void createLeaderboardShouldReturnEmptyWhenDuplicateKey() {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    when(leaderboardsRepository.insert(any(LeaderboardEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<LeaderboardEntity> result = leaderboardsService.createLeaderboard(dto);

    assertFalse(result.isPresent());
    verify(leaderboardsRepository).insert(any(LeaderboardEntity.class));
  }

  @Test
  void getLeaderboardsShouldReturnAllWhenBothParamsNull() {
    List<LeaderboardEntity> entities = List.of(new LeaderboardEntity());
    when(leaderboardsRepository.findAll()).thenReturn(entities);

    List<LeaderboardEntity> result = leaderboardsService.getLeaderboards(null, null);

    assertEquals(1, result.size());
    verify(leaderboardsRepository).findAll();
  }

  @Test
  void getLeaderboardsShouldReturnByGuildIdWhenUserIdNull() {
    List<LeaderboardEntity> entities = List.of(new LeaderboardEntity());
    when(leaderboardsRepository.findByGuildId("g1")).thenReturn(entities);

    List<LeaderboardEntity> result = leaderboardsService.getLeaderboards("g1", null);

    assertEquals(1, result.size());
    verify(leaderboardsRepository).findByGuildId("g1");
  }

  @Test
  void getLeaderboardsShouldReturnByGuildIdAndUserId() {
    List<LeaderboardEntity> entities = List.of(new LeaderboardEntity());
    when(leaderboardsRepository.findByGuildIdAndUserId("g1", "u1"))
      .thenReturn(entities);

    List<LeaderboardEntity> result = leaderboardsService.getLeaderboards("g1", "u1");

    assertEquals(1, result.size());
    verify(leaderboardsRepository).findByGuildIdAndUserId("g1", "u1");
  }

  @Test
  void getLeaderboardByIdShouldReturnEmptyWhenIdNull() {
    Optional<LeaderboardEntity> result = leaderboardsService.getLeaderboardById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getLeaderboardByIdShouldReturnEntityWhenFound() {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb1");
    when(leaderboardsRepository.findById("lb1")).thenReturn(Optional.of(entity));

    Optional<LeaderboardEntity> result = leaderboardsService.getLeaderboardById("lb1");

    assertTrue(result.isPresent());
    assertEquals("lb1", result.get().getId());
    verify(leaderboardsRepository).findById("lb1");
  }

  @Test
  void getLeaderboardByGuildIdAndUserIdShouldReturnEmptyWhenParamsNull() {
    Optional<LeaderboardEntity> result =
      leaderboardsService.getLeaderboardByGuildIdAndUserId(null, "u1");
    assertFalse(result.isPresent());

    result = leaderboardsService.getLeaderboardByGuildIdAndUserId("g1", null);
    assertFalse(result.isPresent());
  }

  @Test
  void getLeaderboardByGuildIdAndUserIdShouldReturnEntityWhenFound() {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb1");
    when(leaderboardsRepository.findByGuildIdAndUserId("g1", "u1"))
      .thenReturn(List.of(entity));

    Optional<LeaderboardEntity> result =
      leaderboardsService.getLeaderboardByGuildIdAndUserId("g1", "u1");

    assertTrue(result.isPresent());
    assertEquals("lb1", result.get().getId());
    verify(leaderboardsRepository).findByGuildIdAndUserId("g1", "u1");
  }

  @Test
  void updateLeaderboardShouldReturnEmptyWhenIdNull() {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");

    Optional<LeaderboardEntity> result =
      leaderboardsService.updateLeaderboard(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateLeaderboardShouldReturnUpdatedEntityWhenFound() {
    LeaderboardEntity existing = new LeaderboardEntity();
    existing.setId("lb1");
    existing.setGuildId("g1");
    existing.setUserId("u1");
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g2");
    dto.setUserId("u2");
    when(leaderboardsRepository.findById("lb1")).thenReturn(Optional.of(existing));
    when(leaderboardsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<LeaderboardEntity> result =
      leaderboardsService.updateLeaderboard("lb1", dto);

    assertTrue(result.isPresent());
    assertEquals("g2", result.get().getGuildId());
    assertEquals("u2", result.get().getUserId());
    verify(leaderboardsRepository).findById("lb1");
    verify(leaderboardsRepository).save(any());
  }

  @Test
  void updateLeaderboardShouldReturnEmptyWhenNotFound() {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    when(leaderboardsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<LeaderboardEntity> result =
      leaderboardsService.updateLeaderboard("missing", dto);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteLeaderboardShouldReturnEmptyWhenIdNull() {
    Optional<LeaderboardEntity> result = leaderboardsService.deleteLeaderboard(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteLeaderboardShouldReturnUpdatedEntityWhenFound() {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb1");
    List<ItemDto> items = new ArrayList<>();
    items.add(new ItemDto("FISH", 5));
    entity.setItems(items);
    when(leaderboardsRepository.findById("lb1")).thenReturn(Optional.of(entity));
    when(leaderboardsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<LeaderboardEntity> result = leaderboardsService.deleteLeaderboard("lb1");

    assertTrue(result.isPresent());
    assertEquals(0, result.get().getItems().get(0).getQuantity());
    verify(leaderboardsRepository).findById("lb1");
    verify(leaderboardsRepository).save(any());
  }

  @Test
  void incrementOrCreateLeaderboardShouldCreateNewWhenEmpty() {
    CreatureDto creature = new CreatureDto("Fish", "Fish", "🐟");
    when(leaderboardsRepository.findByGuildIdAndUserId("g1", "u1"))
      .thenReturn(List.of());
    when(leaderboardsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LeaderboardEntity result =
      leaderboardsService.incrementOrCreateLeaderboard("g1", "u1", creature);

    assertEquals("g1", result.getGuildId());
    assertEquals("u1", result.getUserId());
    assertEquals(1, result.getItems().size());
    assertEquals("FISH", result.getItems().get(0).getName());
    assertEquals(1, result.getItems().get(0).getQuantity());
    verify(leaderboardsRepository).findByGuildIdAndUserId("g1", "u1");
    verify(leaderboardsRepository).save(any());
  }

  @Test
  void incrementOrCreateLeaderboardShouldIncrementExistingCreature() {
    CreatureDto creature = new CreatureDto("Fish", "Fish", "🐟");
    ItemDto item = new ItemDto("FISH", 3);
    LeaderboardEntity existing = new LeaderboardEntity();
    existing.setId("lb1");
    existing.setGuildId("g1");
    existing.setUserId("u1");
    existing.setItems(new ArrayList<>(List.of(item)));
    when(leaderboardsRepository.findByGuildIdAndUserId("g1", "u1"))
      .thenReturn(List.of(existing));
    when(leaderboardsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LeaderboardEntity result =
      leaderboardsService.incrementOrCreateLeaderboard("g1", "u1", creature);

    assertEquals(4, result.getItems().get(0).getQuantity());
    verify(leaderboardsRepository).save(any());
  }

  @Test
  void incrementOrCreateLeaderboardShouldAddNewCreatureToExistingLeaderboard() {
    CreatureDto creature = new CreatureDto("Shark", "Sharks", "🦈");
    ItemDto fishItem = new ItemDto("FISH", 2);
    LeaderboardEntity existing = new LeaderboardEntity();
    existing.setId("lb1");
    existing.setGuildId("g1");
    existing.setUserId("u1");
    existing.setItems(new ArrayList<>(List.of(fishItem)));
    when(leaderboardsRepository.findByGuildIdAndUserId("g1", "u1"))
      .thenReturn(List.of(existing));
    when(leaderboardsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    LeaderboardEntity result =
      leaderboardsService.incrementOrCreateLeaderboard("g1", "u1", creature);

    assertEquals(2, result.getItems().size());
    assertEquals("SHARK", result.getItems().get(1).getName());
    assertEquals(1, result.getItems().get(1).getQuantity());
    verify(leaderboardsRepository).save(any());
  }
}
