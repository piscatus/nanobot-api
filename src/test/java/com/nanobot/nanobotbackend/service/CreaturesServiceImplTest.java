package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.repository.CreaturesRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class CreaturesServiceImplTest {

  @Mock
  private CreaturesRepository creaturesRepository;

  private CreaturesServiceImpl creaturesService;

  @BeforeEach
  void setUp() {
    creaturesService = new CreaturesServiceImpl(creaturesRepository);
  }

  @Test
  void analyzeCreaturesShouldReturnNullWhenInputInvalid() {
    assertNull(creaturesService.analyzeCreatures(null));
    assertNull(creaturesService.analyzeCreatures(""));
    assertNull(creaturesService.analyzeCreatures(" "));
    assertNull(creaturesService.analyzeCreatures("null"));
  }

  @Test
  void analyzeCreaturesShouldReturnCreatureWhenNameMatches() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    entity.setPluralization("Fish");
    entity.setEmoji("🐟");
    when(creaturesRepository.findAll()).thenReturn(List.of(entity));

    CreatureDto result = creaturesService.analyzeCreatures("Fish");

    assertEquals("c1", result.getId());
    assertEquals("Fish", result.getName());
    assertEquals("🐟", result.getEmoji());
    verify(creaturesRepository).findAll();
  }

  @Test
  void analyzeCreaturesShouldReturnCreatureWhenPluralizationMatches() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    entity.setPluralization("Fishes");
    entity.setEmoji("🐟");
    when(creaturesRepository.findAll()).thenReturn(List.of(entity));

    CreatureDto result = creaturesService.analyzeCreatures("Fishes");

    assertEquals("c1", result.getId());
    verify(creaturesRepository).findAll();
  }

  @Test
  void analyzeCreaturesShouldReturnCreatureWhenEmojiMatches() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    entity.setPluralization("Fish");
    entity.setEmoji("🐟");
    when(creaturesRepository.findAll()).thenReturn(List.of(entity));

    CreatureDto result = creaturesService.analyzeCreatures("🐟");

    assertEquals("c1", result.getId());
    verify(creaturesRepository).findAll();
  }

  @Test
  void analyzeCreaturesShouldReturnNullWhenNoMatch() {
    when(creaturesRepository.findAll()).thenReturn(List.of());

    CreatureDto result = creaturesService.analyzeCreatures("Unknown");

    assertNull(result);
    verify(creaturesRepository).findAll();
  }

  @Test
  void setCreaturesShouldAcceptConsumerWithCreatures() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    when(creaturesRepository.findAll()).thenReturn(List.of(entity));

    AtomicReference<List<CreatureDto>> captured = new AtomicReference<>();
    creaturesService.setCreatures(captured::set);

    assertEquals(1, captured.get().size());
    assertEquals("Fish", captured.get().get(0).getName());
    verify(creaturesRepository).findAll();
  }

  @Test
  void createCreatureShouldReturnEntityWhenSuccess() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    CreatureEntity entity = new CreatureEntity(dto);
    when(creaturesRepository.insert(any(CreatureEntity.class))).thenAnswer(inv -> {
      CreatureEntity e = inv.getArgument(0);
      e.setId("new-id");
      return e;
    });

    Optional<CreatureEntity> result = creaturesService.createCreature(dto);

    assertTrue(result.isPresent());
    assertEquals("new-id", result.get().getId());
    verify(creaturesRepository).insert(any(CreatureEntity.class));
  }

  @Test
  void createCreatureShouldReturnEmptyWhenDuplicateKey() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    when(creaturesRepository.insert(any(CreatureEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<CreatureEntity> result = creaturesService.createCreature(dto);

    assertFalse(result.isPresent());
    verify(creaturesRepository).insert(any(CreatureEntity.class));
  }

  @Test
  void createCreatureShouldPropagateOtherExceptions() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    when(creaturesRepository.insert(any(CreatureEntity.class)))
      .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> creaturesService.createCreature(dto));
    verify(creaturesRepository).insert(any(CreatureEntity.class));
  }

  @Test
  void getCreaturesShouldReturnAllFromRepository() {
    List<CreatureEntity> entities = List.of(new CreatureEntity());
    when(creaturesRepository.findAll()).thenReturn(entities);

    List<CreatureEntity> result = creaturesService.getCreatures();

    assertEquals(1, result.size());
    verify(creaturesRepository).findAll();
  }

  @Test
  void getCreatureByIdShouldReturnEmptyWhenIdNull() {
    Optional<CreatureEntity> result = creaturesService.getCreatureById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getCreatureByIdShouldReturnEntityWhenFound() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    when(creaturesRepository.findById("c1")).thenReturn(Optional.of(entity));

    Optional<CreatureEntity> result = creaturesService.getCreatureById("c1");

    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    verify(creaturesRepository).findById("c1");
  }

  @Test
  void getCreatureByIdShouldReturnEmptyWhenNotFound() {
    when(creaturesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<CreatureEntity> result = creaturesService.getCreatureById("missing");

    assertFalse(result.isPresent());
  }

  @Test
  void getCreatureByNameShouldReturnEmptyWhenNameNull() {
    Optional<CreatureEntity> result = creaturesService.getCreatureByName(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getCreatureByNameShouldReturnEntityWhenFound() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    when(creaturesRepository.findByName("Fish")).thenReturn(List.of(entity));

    Optional<CreatureEntity> result = creaturesService.getCreatureByName("Fish");

    assertTrue(result.isPresent());
    assertEquals("Fish", result.get().getName());
    verify(creaturesRepository).findByName("Fish");
  }

  @Test
  void getCreatureByNameShouldReturnEmptyWhenNotFound() {
    when(creaturesRepository.findByName("Unknown")).thenReturn(List.of());

    Optional<CreatureEntity> result = creaturesService.getCreatureByName("Unknown");

    assertFalse(result.isPresent());
  }

  @Test
  void updateCreatureShouldReturnEmptyWhenIdNull() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");

    Optional<CreatureEntity> result = creaturesService.updateCreature(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateCreatureShouldReturnUpdatedEntityWhenFound() {
    CreatureEntity existing = new CreatureEntity();
    existing.setId("c1");
    existing.setName("Fish");
    CreatureDto dto = new CreatureDto("Shark", "Sharks", "🦈");
    when(creaturesRepository.findById("c1")).thenReturn(Optional.of(existing));
    when(creaturesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<CreatureEntity> result = creaturesService.updateCreature("c1", dto);

    assertTrue(result.isPresent());
    assertEquals("Shark", result.get().getName());
    verify(creaturesRepository).findById("c1");
    verify(creaturesRepository).save(any());
  }

  @Test
  void updateCreatureShouldReturnEmptyWhenNotFound() {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    when(creaturesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<CreatureEntity> result = creaturesService.updateCreature("missing", dto);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteCreatureShouldReturnEmptyWhenIdNull() {
    Optional<CreatureEntity> result = creaturesService.deleteCreature(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteCreatureShouldReturnEntityWhenFound() {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    when(creaturesRepository.findById("c1")).thenReturn(Optional.of(entity));

    Optional<CreatureEntity> result = creaturesService.deleteCreature("c1");

    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    verify(creaturesRepository).findById("c1");
    verify(creaturesRepository).deleteById("c1");
  }

  @Test
  void deleteCreatureShouldReturnEmptyWhenNotFound() {
    when(creaturesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<CreatureEntity> result = creaturesService.deleteCreature("missing");

    assertFalse(result.isPresent());
  }
}
