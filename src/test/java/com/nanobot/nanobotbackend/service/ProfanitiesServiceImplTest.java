package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.ProfanityDto;
import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import com.nanobot.nanobotbackend.repository.ProfanitiesRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ProfanitiesServiceImplTest {

  @Mock
  private ProfanitiesRepository profanitiesRepository;

  private ProfanitiesServiceImpl profanitiesService;

  @BeforeEach
  void setUp() {
    profanitiesService = new ProfanitiesServiceImpl(profanitiesRepository);
  }

  @Test
  void createProfanityShouldReturnCreatedEntityOnSuccess() {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("bad");
    dto.setPlural("bads");
    dto.setContains(true);
    ProfanityEntity saved = new ProfanityEntity(dto);
    saved.setId("p1");
    when(profanitiesRepository.insert(any(ProfanityEntity.class))).thenReturn(saved);

    Optional<ProfanityEntity> result = profanitiesService.createProfanity(dto);

    assertTrue(result.isPresent());
    assertEquals("p1", result.get().getId());
    assertEquals("bad", result.get().getSingular());
    assertEquals("bads", result.get().getPlural());
    verify(profanitiesRepository).insert(any(ProfanityEntity.class));
  }

  @Test
  void createProfanityShouldReturnEmptyOnDuplicateKeyException() {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("bad");
    dto.setPlural("bads");
    when(profanitiesRepository.insert(any(ProfanityEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<ProfanityEntity> result = profanitiesService.createProfanity(dto);

    assertFalse(result.isPresent());
    verify(profanitiesRepository).insert(any(ProfanityEntity.class));
  }

  @Test
  void getProfanitiesShouldReturnAllWhenSingularNull() {
    List<ProfanityEntity> entities = List.of(new ProfanityEntity());
    when(profanitiesRepository.findAll()).thenReturn(entities);

    List<ProfanityEntity> result = profanitiesService.getProfanities(null);

    assertEquals(1, result.size());
    verify(profanitiesRepository).findAll();
  }

  @Test
  void getProfanitiesShouldReturnBySingular() {
    List<ProfanityEntity> entities = List.of(new ProfanityEntity());
    when(profanitiesRepository.findBySingular("bad")).thenReturn(entities);

    List<ProfanityEntity> result = profanitiesService.getProfanities("bad");

    assertEquals(1, result.size());
    verify(profanitiesRepository).findBySingular("bad");
  }

  @Test
  void getProfanityByIdShouldReturnEmptyWhenIdNull() {
    Optional<ProfanityEntity> result = profanitiesService.getProfanityById(null);

    assertFalse(result.isPresent());
    verify(profanitiesRepository, org.mockito.Mockito.never()).findById(any());
  }

  @Test
  void getProfanityByIdShouldReturnEntityWhenFound() {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("p1");
    entity.setSingular("bad");
    when(profanitiesRepository.findById("p1")).thenReturn(Optional.of(entity));

    Optional<ProfanityEntity> result = profanitiesService.getProfanityById("p1");

    assertTrue(result.isPresent());
    assertEquals("p1", result.get().getId());
    verify(profanitiesRepository).findById("p1");
  }

  @Test
  void getProfanityBySingularShouldReturnEmptyWhenSingularNull() {
    Optional<ProfanityEntity> result =
      profanitiesService.getProfanityBySingular(null);

    assertFalse(result.isPresent());
    verify(profanitiesRepository, org.mockito.Mockito.never()).findBySingular(any());
  }

  @Test
  void getProfanityBySingularShouldReturnEntityWhenFound() {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("p1");
    entity.setSingular("bad");
    when(profanitiesRepository.findBySingular("bad")).thenReturn(List.of(entity));

    Optional<ProfanityEntity> result =
      profanitiesService.getProfanityBySingular("bad");

    assertTrue(result.isPresent());
    assertEquals("bad", result.get().getSingular());
    verify(profanitiesRepository).findBySingular("bad");
  }

  @Test
  void updateProfanityShouldReturnEmptyWhenIdNull() {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("updated");
    dto.setPlural("updateds");

    Optional<ProfanityEntity> result =
      profanitiesService.updateProfanity(null, dto);

    assertFalse(result.isPresent());
    verify(profanitiesRepository, org.mockito.Mockito.never()).findById(any());
  }

  @Test
  void updateProfanityShouldReturnUpdatedEntityWhenFound() {
    ProfanityEntity existing = new ProfanityEntity();
    existing.setId("p1");
    existing.setSingular("bad");
    existing.setPlural("bads");
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("worse");
    dto.setPlural("worses");
    dto.setContains(false);
    when(profanitiesRepository.findById("p1")).thenReturn(Optional.of(existing));
    when(profanitiesRepository.save(any(ProfanityEntity.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    Optional<ProfanityEntity> result =
      profanitiesService.updateProfanity("p1", dto);

    assertTrue(result.isPresent());
    assertEquals("worse", result.get().getSingular());
    assertEquals("worses", result.get().getPlural());
    verify(profanitiesRepository).findById("p1");
    verify(profanitiesRepository).save(any(ProfanityEntity.class));
  }

  @Test
  void deleteProfanityShouldReturnEmptyWhenIdNull() {
    Optional<ProfanityEntity> result = profanitiesService.deleteProfanity(null);

    assertFalse(result.isPresent());
    verify(profanitiesRepository, org.mockito.Mockito.never()).deleteById(any());
  }

  @Test
  void deleteProfanityShouldReturnDeletedEntityWhenFound() {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("p1");
    entity.setSingular("bad");
    when(profanitiesRepository.findById("p1")).thenReturn(Optional.of(entity));

    Optional<ProfanityEntity> result = profanitiesService.deleteProfanity("p1");

    assertTrue(result.isPresent());
    assertEquals("p1", result.get().getId());
    verify(profanitiesRepository).findById("p1");
    verify(profanitiesRepository).deleteById("p1");
  }

  @Test
  void deleteProfanityShouldReturnEmptyWhenNotFound() {
    when(profanitiesRepository.findById("nonexistent")).thenReturn(Optional.empty());

    Optional<ProfanityEntity> result =
      profanitiesService.deleteProfanity("nonexistent");

    assertFalse(result.isPresent());
    verify(profanitiesRepository).findById("nonexistent");
    verify(profanitiesRepository, org.mockito.Mockito.never()).deleteById(any());
  }
}
