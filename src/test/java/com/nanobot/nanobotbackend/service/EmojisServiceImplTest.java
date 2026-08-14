package com.nanobot.nanobotbackend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.EmojiDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.repository.EmojisRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class EmojisServiceImplTest {

  @Mock
  private EmojisRepository emojisRepository;

  private EmojisServiceImpl emojisService;

  @BeforeEach
  void setUp() {
    emojisService = new EmojisServiceImpl(emojisRepository);
  }

  @Test
  void createEmojiShouldReturnCreatedEntityOnSuccess() {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    dto.setEmoji("🐟");
    EmojiEntity saved = new EmojiEntity();
    saved.setId("e1");
    saved.setCategory("fish");
    saved.setName("trout");
    saved.setEmoji("🐟");
    when(emojisRepository.insert(any(EmojiEntity.class))).thenReturn(saved);

    Optional<EmojiEntity> result = emojisService.createEmoji(dto);

    assertTrue(result.isPresent());
    assertEquals("e1", result.get().getId());
    assertEquals("fish", result.get().getCategory());
    assertEquals("trout", result.get().getName());
    verify(emojisRepository).insert(any(EmojiEntity.class));
  }

  @Test
  void createEmojiShouldPropagateExceptionWhenNonDuplicateKeyException() {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    dto.setEmoji("🐟");
    when(emojisRepository.insert(any(EmojiEntity.class)))
      .thenThrow(new RuntimeException("DB connection failed"));

    assertThrows(RuntimeException.class, () -> emojisService.createEmoji(dto));
    verify(emojisRepository).insert(any(EmojiEntity.class));
  }

  @Test
  void createEmojiShouldReturnEmptyOnDuplicateKeyException() {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    dto.setEmoji("🐟");
    when(emojisRepository.insert(any(EmojiEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));

    Optional<EmojiEntity> result = emojisService.createEmoji(dto);

    assertFalse(result.isPresent());
    verify(emojisRepository).insert(any(EmojiEntity.class));
  }

  @Test
  void getEmojisShouldReturnAllWhenNoParams() {
    List<EmojiEntity> entities = List.of(new EmojiEntity());
    when(emojisRepository.findAll()).thenReturn(entities);

    List<EmojiEntity> result = emojisService.getEmojis(null, null);

    assertEquals(1, result.size());
    verify(emojisRepository).findAll();
  }

  @Test
  void getEmojisShouldReturnByCategoryAndNameWhenBothProvided() {
    List<EmojiEntity> entities = List.of(new EmojiEntity("fish", "trout", "🐟"));
    when(emojisRepository.findByCategoryAndName("fish", "trout")).thenReturn(entities);

    List<EmojiEntity> result = emojisService.getEmojis("fish", "trout");

    assertEquals(1, result.size());
    assertEquals("fish", result.get(0).getCategory());
    assertEquals("trout", result.get(0).getName());
    verify(emojisRepository).findByCategoryAndName("fish", "trout");
  }

  @Test
  void getEmojisShouldReturnByCategoryWhenOnlyCategoryProvided() {
    List<EmojiEntity> entities = List.of(new EmojiEntity("fish", "trout", "🐟"));
    when(emojisRepository.findByCategory("fish")).thenReturn(entities);

    List<EmojiEntity> result = emojisService.getEmojis("fish", null);

    assertEquals(1, result.size());
    verify(emojisRepository).findByCategory("fish");
  }

  @Test
  void getEmojisShouldReturnByNameWhenOnlyNameProvided() {
    List<EmojiEntity> entities = List.of(new EmojiEntity("fish", "trout", "🐟"));
    when(emojisRepository.findByName("trout")).thenReturn(entities);

    List<EmojiEntity> result = emojisService.getEmojis(null, "trout");

    assertEquals(1, result.size());
    verify(emojisRepository).findByName("trout");
  }

  @Test
  void getEmojiByIdShouldReturnEmptyWhenIdIsNull() {
    Optional<EmojiEntity> result = emojisService.getEmojiById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getEmojiByIdShouldReturnEntityWhenFound() {
    EmojiEntity entity = new EmojiEntity("fish", "trout", "🐟");
    entity.setId("e1");
    when(emojisRepository.findById("e1")).thenReturn(Optional.of(entity));

    Optional<EmojiEntity> result = emojisService.getEmojiById("e1");

    assertTrue(result.isPresent());
    assertEquals("e1", result.get().getId());
    verify(emojisRepository).findById("e1");
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnEmptyWhenNameNull() {
    Optional<EmojiEntity> result = emojisService.getEmojiByCategoryAndName("fish", null);

    assertFalse(result.isPresent());
    verify(emojisRepository, never()).findByCategoryAndName(any(), any());
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnEmptyWhenEmptyList() {
    when(emojisRepository.findByCategoryAndName("fish", "trout")).thenReturn(List.of());

    Optional<EmojiEntity> result = emojisService.getEmojiByCategoryAndName("fish", "trout");

    assertFalse(result.isPresent());
    verify(emojisRepository).findByCategoryAndName("fish", "trout");
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnEmptyWhenMultipleFound() {
    EmojiEntity first = new EmojiEntity("fish", "trout", "🐟");
    first.setId("e1");
    EmojiEntity second = new EmojiEntity("fish", "trout", "🐟");
    second.setId("e2");
    when(emojisRepository.findByCategoryAndName("fish", "trout"))
      .thenReturn(List.of(first, second));

    Optional<EmojiEntity> result = emojisService.getEmojiByCategoryAndName("fish", "trout");

    assertFalse(result.isPresent());
    verify(emojisRepository).findByCategoryAndName("fish", "trout");
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnEmptyWhenCategoryNull() {
    Optional<EmojiEntity> result = emojisService.getEmojiByCategoryAndName(null, "trout");

    assertFalse(result.isPresent());
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnEntityWhenFound() {
    List<EmojiEntity> entities = List.of(new EmojiEntity("fish", "trout", "🐟"));
    when(emojisRepository.findByCategoryAndName("fish", "trout")).thenReturn(entities);

    Optional<EmojiEntity> result = emojisService.getEmojiByCategoryAndName("fish", "trout");

    assertTrue(result.isPresent());
    assertEquals("fish", result.get().getCategory());
    assertEquals("trout", result.get().getName());
  }

  @Test
  void updateEmojiShouldReturnEmptyWhenIdIsNull() {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");

    Optional<EmojiEntity> result = emojisService.updateEmoji(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateEmojiShouldReturnUpdatedEntityWhenFound() {
    EmojiEntity existing = new EmojiEntity("fish", "trout", "🐟");
    existing.setId("e1");
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("salmon");
    dto.setEmoji("🐟");
    when(emojisRepository.findById("e1")).thenReturn(Optional.of(existing));
    when(emojisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<EmojiEntity> result = emojisService.updateEmoji("e1", dto);

    assertTrue(result.isPresent());
    assertEquals("salmon", result.get().getName());
    verify(emojisRepository).findById("e1");
    verify(emojisRepository).save(any());
  }

  @Test
  void updateEmojiShouldReturnEmptyWhenNotFound() {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    when(emojisRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<EmojiEntity> result = emojisService.updateEmoji("missing", dto);

    assertFalse(result.isPresent());
    verify(emojisRepository).findById("missing");
  }

  @Test
  void deleteEmojiShouldReturnEmptyWhenIdIsNull() {
    Optional<EmojiEntity> result = emojisService.deleteEmoji(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteEmojiShouldReturnDeletedEntityWhenFound() {
    EmojiEntity entity = new EmojiEntity("fish", "trout", "🐟");
    entity.setId("e1");
    when(emojisRepository.findById("e1")).thenReturn(Optional.of(entity));

    Optional<EmojiEntity> result = emojisService.deleteEmoji("e1");

    assertTrue(result.isPresent());
    assertEquals("e1", result.get().getId());
    verify(emojisRepository).findById("e1");
    verify(emojisRepository).deleteById("e1");
  }

  @Test
  void deleteEmojiShouldReturnEmptyWhenNotFound() {
    when(emojisRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<EmojiEntity> result = emojisService.deleteEmoji("missing");

    assertFalse(result.isPresent());
    verify(emojisRepository).findById("missing");
  }
}
