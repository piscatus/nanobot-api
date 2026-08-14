package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.repository.PickupsRepository;
import java.util.Date;
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
class PickupsServiceImplTest {

  @Mock
  private PickupsRepository pickupsRepository;

  private PickupsServiceImpl pickupsService;

  @BeforeEach
  void setUp() {
    pickupsService = new PickupsServiceImpl(pickupsRepository);
  }

  @Test
  void createPickupShouldReturnCreatedEntity() {
    PickupDto dto = new PickupDto(null, "drop1", "user1", new Date());
    when(pickupsRepository.insert(any(PickupEntity.class)))
      .thenAnswer((Answer<PickupEntity>) inv -> {
        PickupEntity e = inv.getArgument(0);
        e.setId("pickup-1");
        return e;
      });

    Optional<PickupEntity> result = pickupsService.createPickup(dto);

    assertTrue(result.isPresent());
    assertEquals("pickup-1", result.get().getId());
    assertEquals("drop1", result.get().getDropId());
    assertEquals("user1", result.get().getUserId());
    verify(pickupsRepository).insert(any(PickupEntity.class));
  }

  @Test
  void createPickupShouldReturnEmptyOnDuplicateKey() {
    PickupDto dto = new PickupDto(null, "drop1", "user1", new Date());
    when(pickupsRepository.insert(any(PickupEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<PickupEntity> result = pickupsService.createPickup(dto);

    assertFalse(result.isPresent());
    verify(pickupsRepository).insert(any(PickupEntity.class));
  }

  @Test
  void createPickupShouldPropagateOtherExceptions() {
    PickupDto dto = new PickupDto(null, "drop1", "user1", new Date());
    when(pickupsRepository.insert(any(PickupEntity.class)))
      .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> pickupsService.createPickup(dto));
    verify(pickupsRepository).insert(any(PickupEntity.class));
  }

  @Test
  void getPickupsShouldReturnAllWhenMessageIdNull() {
    List<PickupEntity> entities = List.of(new PickupEntity());
    when(pickupsRepository.findAll()).thenReturn(entities);

    List<PickupEntity> result = pickupsService.getPickups(null, null);

    assertEquals(1, result.size());
    verify(pickupsRepository).findAll();
  }

  @Test
  void getPickupsShouldReturnByDropIdWhenUserIdNull() {
    List<PickupEntity> entities = List.of(new PickupEntity());
    when(pickupsRepository.findByDropId("drop1")).thenReturn(entities);

    List<PickupEntity> result = pickupsService.getPickups("drop1", null);

    assertEquals(1, result.size());
    verify(pickupsRepository).findByDropId("drop1");
  }

  @Test
  void getPickupsShouldReturnByDropIdAndUserId() {
    List<PickupEntity> entities = List.of(new PickupEntity());
    when(pickupsRepository.findByDropIdAndUserId("drop1", "user1"))
      .thenReturn(entities);

    List<PickupEntity> result = pickupsService.getPickups("drop1", "user1");

    assertEquals(1, result.size());
    verify(pickupsRepository).findByDropIdAndUserId("drop1", "user1");
  }

  @Test
  void getPickupByIdShouldReturnEmptyWhenIdNull() {
    Optional<PickupEntity> result = pickupsService.getPickupById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getPickupByIdShouldReturnEntityWhenFound() {
    PickupEntity entity = new PickupEntity();
    entity.setId("p1");
    entity.setDropId("drop1");
    when(pickupsRepository.findById("p1")).thenReturn(Optional.of(entity));

    Optional<PickupEntity> result = pickupsService.getPickupById("p1");

    assertTrue(result.isPresent());
    assertEquals("p1", result.get().getId());
    verify(pickupsRepository).findById("p1");
  }

  @Test
  void getPickupByDropIdAndUserIdShouldReturnEmptyWhenDropIdNull() {
    Optional<PickupEntity> result =
      pickupsService.getPickupByDropIdAndUserId(null, "user1");

    assertFalse(result.isPresent());
  }

  @Test
  void getPickupByDropIdAndUserIdShouldReturnEntityWhenFound() {
    PickupEntity entity = new PickupEntity();
    entity.setId("p1");
    entity.setDropId("drop1");
    entity.setUserId("user1");
    when(pickupsRepository.findByDropIdAndUserId("drop1", "user1"))
      .thenReturn(List.of(entity));

    Optional<PickupEntity> result =
      pickupsService.getPickupByDropIdAndUserId("drop1", "user1");

    assertTrue(result.isPresent());
    assertEquals("drop1", result.get().getDropId());
    assertEquals("user1", result.get().getUserId());
    verify(pickupsRepository).findByDropIdAndUserId("drop1", "user1");
  }

  @Test
  void updatePickupShouldReturnUpdatedEntityWhenFound() {
    PickupEntity existing = new PickupEntity();
    existing.setId("p1");
    existing.setDropId("drop1");
    PickupDto dto = new PickupDto("p1", "drop2", "user2", new Date());
    when(pickupsRepository.findById("p1")).thenReturn(Optional.of(existing));
    when(pickupsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<PickupEntity> result = pickupsService.updatePickup("p1", dto);

    assertTrue(result.isPresent());
    assertEquals("drop2", result.get().getDropId());
    assertEquals("user2", result.get().getUserId());
    verify(pickupsRepository).findById("p1");
    verify(pickupsRepository).save(any());
  }

  @Test
  void updatePickupShouldReturnEmptyWhenNotFound() {
    PickupDto dto = new PickupDto("p1", "drop1", "user1", new Date());
    when(pickupsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<PickupEntity> result =
      pickupsService.updatePickup("missing", dto);

    assertFalse(result.isPresent());
  }

  @Test
  void deletePickupShouldReturnDeletedEntityWhenFound() {
    PickupEntity entity = new PickupEntity();
    entity.setId("p1");
    when(pickupsRepository.findById("p1")).thenReturn(Optional.of(entity));

    Optional<PickupEntity> result = pickupsService.deletePickup("p1");

    assertTrue(result.isPresent());
    assertEquals("p1", result.get().getId());
    verify(pickupsRepository).findById("p1");
    verify(pickupsRepository).deleteById("p1");
  }

  @Test
  void deletePickupShouldReturnEmptyWhenNotFound() {
    when(pickupsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<PickupEntity> result = pickupsService.deletePickup("missing");

    assertFalse(result.isPresent());
  }
}
