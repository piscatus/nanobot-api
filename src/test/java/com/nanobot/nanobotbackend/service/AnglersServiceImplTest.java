package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import com.nanobot.nanobotbackend.repository.AnglersRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AnglersServiceImplTest {

  @Mock
  private AnglersRepository anglersRepository;

  private AnglersServiceImpl anglersService;

  @BeforeEach
  void setUp() {
    anglersService = new AnglersServiceImpl(anglersRepository);
  }

  @Test
  void createAnglerShouldReturnCreatedWhenSuccess() {
    AnglerDto dto = new AnglerDto();
    dto.setGuildId("guild1");
    dto.setUserId("user1");
    dto.setResting(false);
    dto.setTimestamp(new Date());
    AnglerEntity entity = new AnglerEntity(dto);
    entity.setId("id-1");
    when(anglersRepository.insert(any(AnglerEntity.class))).thenReturn(entity);

    Optional<AnglerEntity> result = anglersService.createAngler(dto);

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(anglersRepository).insert(any(AnglerEntity.class));
  }

  @Test
  void createAnglerShouldReturnEmptyWhenDuplicateKey() {
    AnglerDto dto = new AnglerDto();
    dto.setGuildId("guild1");
    dto.setUserId("user1");
    when(anglersRepository.insert(any(AnglerEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));

    Optional<AnglerEntity> result = anglersService.createAngler(dto);

    assertTrue(result.isEmpty());
    verify(anglersRepository).insert(any(AnglerEntity.class));
  }

  @Test
  void getAnglersShouldReturnListWhenGuildAndUserIdProvided() {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersRepository.findByGuildIdAndUserId("guild1", "user1")).thenReturn(List.of(entity));

    List<AnglerEntity> result = anglersService.getAnglers("guild1", "user1");

    assertFalse(result.isEmpty());
    assertEquals("id-1", result.get(0).getId());
    verify(anglersRepository).findByGuildIdAndUserId("guild1", "user1");
  }

  @Test
  void getAnglersShouldReturnAllWhenBothNull() {
    AnglerEntity entity = new AnglerEntity();
    when(anglersRepository.findAll()).thenReturn(List.of(entity));

    List<AnglerEntity> result = anglersService.getAnglers(null, null);

    assertFalse(result.isEmpty());
    verify(anglersRepository).findAll();
  }

  @Test
  void getRestingAnglersShouldReturnList() {
    AnglerEntity entity = new AnglerEntity();
    when(anglersRepository.findAllRestingAnglers()).thenReturn(List.of(entity));

    List<AnglerEntity> result = anglersService.getRestingAnglers();

    assertFalse(result.isEmpty());
    verify(anglersRepository).findAllRestingAnglers();
  }

  @Test
  void getAnglerByIdShouldReturnPresentWhenFound() {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersRepository.findById("id-1")).thenReturn(Optional.of(entity));

    Optional<AnglerEntity> result = anglersService.getAnglerById("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(anglersRepository).findById("id-1");
  }

  @Test
  void getAnglerByIdShouldReturnEmptyWhenNotFound() {
    when(anglersRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<AnglerEntity> result = anglersService.getAnglerById("missing");

    assertTrue(result.isEmpty());
    verify(anglersRepository).findById("missing");
  }

  @Test
  void getAnglerByIdShouldReturnEmptyWhenIdIsNull() {
    Optional<AnglerEntity> result = anglersService.getAnglerById(null);

    assertTrue(result.isEmpty());
    verify(anglersRepository, never()).findById(any());
  }

  @Test
  void getAnglerByGuildIdAndUserIdShouldReturnPresentWhenFound() {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersRepository.findByGuildIdAndUserId("guild1", "user1")).thenReturn(List.of(entity));

    Optional<AnglerEntity> result = anglersService.getAnglerByGuildIdAndUserId("guild1", "user1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(anglersRepository).findByGuildIdAndUserId("guild1", "user1");
  }

  @Test
  void getAnglerByGuildIdAndUserIdShouldReturnEmptyWhenNotFound() {
    when(anglersRepository.findByGuildIdAndUserId("guild1", "user1")).thenReturn(List.of());

    Optional<AnglerEntity> result = anglersService.getAnglerByGuildIdAndUserId("guild1", "user1");

    assertTrue(result.isEmpty());
    verify(anglersRepository).findByGuildIdAndUserId("guild1", "user1");
  }

  @Test
  void getAnglerByGuildIdAndUserIdShouldReturnEmptyWhenGuildOrUserIdNull() {
    Optional<AnglerEntity> result1 = anglersService.getAnglerByGuildIdAndUserId(null, "user1");
    Optional<AnglerEntity> result2 = anglersService.getAnglerByGuildIdAndUserId("guild1", null);

    assertTrue(result1.isEmpty());
    assertTrue(result2.isEmpty());
    verify(anglersRepository, never()).findByGuildIdAndUserId(any(), any());
  }
}
