package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.repository.DropsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class DropsServiceImplTest {

  @Mock
  private DropsRepository dropsRepository;

  private DropsServiceImpl dropsService;

  @BeforeEach
  void setUp() {
    dropsService = new DropsServiceImpl(dropsRepository);
  }

  @Test
  void createDropShouldReturnCreatedEntityOnSuccess() {
    DropDto dto = new DropDto();
    dto.setMessageId("msg1");
    dto.setGuildId("g1");
    dto.setChannelId("c1");
    dto.setUserId("u1");
    DropEntity saved = new DropEntity();
    saved.setId("d1");
    saved.setMessageId("msg1");
    when(dropsRepository.insert(any(DropEntity.class))).thenReturn(saved);

    Optional<DropEntity> result = dropsService.createDrop(dto);

    assertTrue(result.isPresent());
    assertEquals("d1", result.get().getId());
    verify(dropsRepository).insert(any(DropEntity.class));
  }

  @Test
  void createDropShouldReturnEmptyOnDuplicateKeyException() {
    DropDto dto = new DropDto();
    dto.setMessageId("msg1");
    when(dropsRepository.insert(any(DropEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));

    Optional<DropEntity> result = dropsService.createDrop(dto);

    assertFalse(result.isPresent());
    verify(dropsRepository).insert(any(DropEntity.class));
  }

  @Test
  void createDropShouldPropagateExceptionWhenNonDuplicateKeyException() {
    DropDto dto = new DropDto();
    dto.setMessageId("msg1");
    when(dropsRepository.insert(any(DropEntity.class)))
      .thenThrow(new RuntimeException("DB connection failed"));

    assertThrows(RuntimeException.class, () -> dropsService.createDrop(dto));
    verify(dropsRepository).insert(any(DropEntity.class));
  }

  @Test
  void getDropsShouldReturnAllWhenMessageIdNull() {
    List<DropEntity> entities = List.of(new DropEntity());
    when(dropsRepository.findAll()).thenReturn(entities);

    List<DropEntity> result = dropsService.getDrops(null);

    assertEquals(1, result.size());
    verify(dropsRepository).findAll();
  }

  @Test
  void getDropsShouldReturnByMessageIdWhenProvided() {
    List<DropEntity> entities = List.of(new DropEntity());
    when(dropsRepository.findByMessageId("msg1")).thenReturn(entities);

    List<DropEntity> result = dropsService.getDrops("msg1");

    assertEquals(1, result.size());
    verify(dropsRepository).findByMessageId("msg1");
  }

  @Test
  void getDropByIdShouldReturnEmptyWhenIdNull() {
    Optional<DropEntity> result = dropsService.getDropById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getDropByIdShouldReturnEntityWhenFound() {
    DropEntity entity = new DropEntity();
    entity.setId("d1");
    entity.setMessageId("msg1");
    when(dropsRepository.findById("d1")).thenReturn(Optional.of(entity));

    Optional<DropEntity> result = dropsService.getDropById("d1");

    assertTrue(result.isPresent());
    assertEquals("d1", result.get().getId());
    verify(dropsRepository).findById("d1");
  }

  @Test
  void updateDropMessageIdShouldReturnEmptyWhenIdInvalid() {
    RequestDto request = createRequestDto(null, "msg1");

    Optional<DropEntity> result = dropsService.updateDropMessageId(request);

    assertFalse(result.isPresent());
  }

  @Test
  void updateDropMessageIdShouldReturnUpdatedEntityWhenValid() {
    RequestDto request = createRequestDto("d1", "msg2");
    DropEntity existing = new DropEntity();
    existing.setId("d1");
    existing.setMessageId("msg1");
    when(dropsRepository.findById("d1")).thenReturn(Optional.of(existing));
    when(dropsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<DropEntity> result = dropsService.updateDropMessageId(request);

    assertTrue(result.isPresent());
    assertEquals("msg2", result.get().getMessageId());
    verify(dropsRepository).findById("d1");
    verify(dropsRepository).save(any());
  }

  @Test
  void updateDropMessageIdShouldReturnEmptyWhenDropNotFound() {
    RequestDto request = createRequestDto("d1", "msg2");
    when(dropsRepository.findById("d1")).thenReturn(Optional.empty());

    Optional<DropEntity> result = dropsService.updateDropMessageId(request);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteDropShouldReturnEmptyWhenIdNull() {
    Optional<DropEntity> result = dropsService.deleteDrop(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteDropShouldReturnDeletedEntityWhenFound() {
    DropEntity entity = new DropEntity();
    entity.setId("d1");
    when(dropsRepository.findById("d1")).thenReturn(Optional.of(entity));

    Optional<DropEntity> result = dropsService.deleteDrop("d1");

    assertTrue(result.isPresent());
    assertEquals("d1", result.get().getId());
    verify(dropsRepository).findById("d1");
    verify(dropsRepository).deleteById("d1");
  }

  @Test
  void getDropByMessageIdShouldReturnEmptyWhenMessageIdNull() {
    Optional<DropEntity> result = dropsService.getDropByMessageId(null);

    assertFalse(result.isPresent());
    verify(dropsRepository, never()).findByMessageId(any());
  }

  @Test
  void getDropByMessageIdShouldReturnEmptyWhenNoDropsFound() {
    when(dropsRepository.findByMessageId("msg1")).thenReturn(List.of());

    Optional<DropEntity> result = dropsService.getDropByMessageId("msg1");

    assertFalse(result.isPresent());
    verify(dropsRepository).findByMessageId("msg1");
  }

  @Test
  void getDropByMessageIdShouldReturnFirstWhenSingleDropFound() {
    DropEntity entity = new DropEntity();
    entity.setId("d1");
    entity.setMessageId("msg1");
    when(dropsRepository.findByMessageId("msg1")).thenReturn(List.of(entity));

    Optional<DropEntity> result = dropsService.getDropByMessageId("msg1");

    assertTrue(result.isPresent());
    assertEquals("d1", result.get().getId());
    assertEquals("msg1", result.get().getMessageId());
    verify(dropsRepository).findByMessageId("msg1");
  }

  @Test
  void getDropByMessageIdShouldReturnEmptyWhenMultipleDropsFound() {
    DropEntity first = new DropEntity();
    first.setId("d1");
    first.setMessageId("msg1");
    DropEntity second = new DropEntity();
    second.setId("d2");
    second.setMessageId("msg1");
    when(dropsRepository.findByMessageId("msg1")).thenReturn(List.of(first, second));

    Optional<DropEntity> result = dropsService.getDropByMessageId("msg1");

    assertFalse(result.isPresent());
    verify(dropsRepository).findByMessageId("msg1");
  }

  @Test
  void updateDropShouldReturnEmptyWhenIdNull() {
    DropDto dto = new DropDto();
    dto.setMessageId("msg1");

    Optional<DropEntity> result = dropsService.updateDrop(null, dto);

    assertFalse(result.isPresent());
    verify(dropsRepository, never()).findById(any());
  }

  @Test
  void updateDropShouldReturnEmptyWhenNotFound() {
    DropDto dto = new DropDto();
    dto.setMessageId("msg2");
    dto.setGuildId("g1");
    when(dropsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<DropEntity> result = dropsService.updateDrop("missing", dto);

    assertFalse(result.isPresent());
    verify(dropsRepository).findById("missing");
    verify(dropsRepository, never()).save(any());
  }

  @Test
  void updateDropShouldReturnUpdatedEntityWhenFound() {
    DropEntity existing = new DropEntity();
    existing.setId("d1");
    existing.setMessageId("msg1");
    existing.setGuildId("g1");
    existing.setChannelId("c1");
    existing.setUserId("u1");
    DropDto dto = new DropDto();
    dto.setMessageId("msg2");
    dto.setGuildId("g2");
    dto.setChannelId("c2");
    dto.setUserId("u2");
    dto.setInput("100");
    dto.setMaximumEntries("5");
    dto.setNumberWinners(2);
    when(dropsRepository.findById("d1")).thenReturn(Optional.of(existing));
    when(dropsRepository.save(any(DropEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<DropEntity> result = dropsService.updateDrop("d1", dto);

    assertTrue(result.isPresent());
    assertEquals("msg2", result.get().getMessageId());
    assertEquals("g2", result.get().getGuildId());
    assertEquals("c2", result.get().getChannelId());
    assertEquals("u2", result.get().getUserId());
    assertEquals("100", result.get().getInput());
    assertEquals("5", result.get().getMaximumEntries());
    assertEquals(2, result.get().getNumberWinners());
    verify(dropsRepository).findById("d1");
    verify(dropsRepository).save(any(DropEntity.class));
  }

  @Test
  void setDropsShouldPassDtosToConsumer() {
    DropEntity entity = new DropEntity();
    entity.setId("d1");
    entity.setMessageId("msg1");
    entity.setGuildId("g1");
    when(dropsRepository.findAll()).thenReturn(List.of(entity));

    java.util.List<DropDto> captured = new java.util.ArrayList<>();
    dropsService.setDrops(captured::addAll);

    assertEquals(1, captured.size());
    assertEquals("d1", captured.get(0).getId());
    assertEquals("msg1", captured.get(0).getMessageId());
    verify(dropsRepository).findAll();
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
}
