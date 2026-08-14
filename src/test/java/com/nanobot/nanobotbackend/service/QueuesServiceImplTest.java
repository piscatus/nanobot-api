package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.repository.QueuesRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueuesServiceImplTest {

  @Mock
  private QueuesRepository queuesRepository;

  private QueuesServiceImpl queuesService;

  @BeforeEach
  void setUp() {
    queuesService = new QueuesServiceImpl(queuesRepository);
  }

  @Test
  void createQueueShouldReturnCreatedEntity() {
    QueueDto dto = new QueueDto("user1", "src-addr", "tgt-addr", LevelDto.SEND, null, "100", "XNO", false, "seed", new Date(), null);
    QueueEntity entity = new QueueEntity(dto);
    entity.setId("id-1");
    when(queuesRepository.insert(any(QueueEntity.class))).thenReturn(entity);

    QueueEntity result = queuesService.createQueue(dto);

    assertNotNull(result);
    assertEquals("id-1", result.getId());
    verify(queuesRepository).insert(any(QueueEntity.class));
  }

  @Test
  void getQueuesShouldReturnList() {
    QueueEntity entity = new QueueEntity();
    entity.setId("id-1");
    when(queuesRepository.findAllSortedByOldestTimestamp()).thenReturn(List.of(entity));

    List<QueueEntity> result = queuesService.getQueues();

    assertFalse(result.isEmpty());
    assertEquals("id-1", result.get(0).getId());
    verify(queuesRepository).findAllSortedByOldestTimestamp();
  }

  @Test
  void getQueueByIdShouldReturnPresentWhenFound() {
    QueueEntity entity = new QueueEntity();
    entity.setId("id-1");
    when(queuesRepository.findById("id-1")).thenReturn(Optional.of(entity));

    Optional<QueueEntity> result = queuesService.getQueueById("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(queuesRepository).findById("id-1");
  }

  @Test
  void getQueueByIdShouldReturnEmptyWhenNotFound() {
    when(queuesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<QueueEntity> result = queuesService.getQueueById("missing");

    assertTrue(result.isEmpty());
    verify(queuesRepository).findById("missing");
  }

  @Test
  void getQueueByIdShouldReturnEmptyWhenIdIsNull() {
    Optional<QueueEntity> result = queuesService.getQueueById(null);

    assertTrue(result.isEmpty());
    verify(queuesRepository, never()).findById(any());
  }

  @Test
  void updateQueueShouldReturnUpdatedWhenFound() {
    QueueEntity existing = new QueueEntity();
    existing.setId("id-1");
    QueueDto dto = new QueueDto("user2", "src2", "tgt2", LevelDto.RECEIVE, "hash", "200", "BAN", true, "seed2", new Date(), "tx-1");
    when(queuesRepository.findById("id-1")).thenReturn(Optional.of(existing));
    when(queuesRepository.save(any(QueueEntity.class))).thenReturn(existing);

    Optional<QueueEntity> result = queuesService.updateQueue("id-1", dto);

    assertTrue(result.isPresent());
    verify(queuesRepository).findById("id-1");
    verify(queuesRepository).save(any(QueueEntity.class));
  }

  @Test
  void updateQueueShouldReturnEmptyWhenNotFound() {
    QueueDto dto = new QueueDto(null, null, null, null, null, null, null, false, null, null, null);
    when(queuesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<QueueEntity> result = queuesService.updateQueue("missing", dto);

    assertTrue(result.isEmpty());
    verify(queuesRepository).findById("missing");
    verify(queuesRepository, never()).save(any());
  }

  @Test
  void deleteQueueShouldReturnDeletedWhenFound() {
    QueueEntity entity = new QueueEntity();
    entity.setId("id-1");
    when(queuesRepository.findById("id-1")).thenReturn(Optional.of(entity));
    doNothing().when(queuesRepository).deleteById("id-1");

    Optional<QueueEntity> result = queuesService.deleteQueue("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(queuesRepository).findById("id-1");
    verify(queuesRepository).deleteById("id-1");
  }

  @Test
  void deleteQueueShouldReturnEmptyWhenNotFound() {
    when(queuesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<QueueEntity> result = queuesService.deleteQueue("missing");

    assertTrue(result.isEmpty());
    verify(queuesRepository).findById("missing");
    verify(queuesRepository, never()).deleteById(any());
  }
}
