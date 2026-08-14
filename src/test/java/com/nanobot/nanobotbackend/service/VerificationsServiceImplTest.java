package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.VerificationDto;
import com.nanobot.nanobotbackend.entity.VerificationEntity;
import com.nanobot.nanobotbackend.repository.VerificationsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class VerificationsServiceImplTest {

  @Mock
  private VerificationsRepository verificationsRepository;

  private VerificationsServiceImpl verificationsService;

  @BeforeEach
  void setUp() {
    verificationsService = new VerificationsServiceImpl(verificationsRepository);
  }

  @Test
  void createVerificationShouldReturnCreatedEntityOnSuccess() {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("user-1");
    VerificationEntity saved = new VerificationEntity();
    saved.setId("v1");
    saved.setUserId("user-1");
    when(verificationsRepository.insert(any(VerificationEntity.class)))
      .thenReturn(saved);

    Optional<VerificationEntity> result =
      verificationsService.createVerification(dto);

    assertTrue(result.isPresent());
    assertEquals("v1", result.get().getId());
    assertEquals("user-1", result.get().getUserId());
    verify(verificationsRepository).insert(any(VerificationEntity.class));
  }

  @Test
  void createVerificationShouldReturnEmptyOnDuplicateKeyException() {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("user-1");
    when(verificationsRepository.insert(any(VerificationEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<VerificationEntity> result =
      verificationsService.createVerification(dto);

    assertFalse(result.isPresent());
    verify(verificationsRepository).insert(any(VerificationEntity.class));
  }

  @Test
  void getVerificationsShouldReturnAllWhenUserIdIsNull() {
    List<VerificationEntity> entities = List.of(new VerificationEntity());
    when(verificationsRepository.findAll()).thenReturn(entities);

    List<VerificationEntity> result = verificationsService.getVerifications(null);

    assertEquals(1, result.size());
    verify(verificationsRepository).findAll();
  }

  @Test
  void getVerificationsShouldReturnByUserIdWhenProvided() {
    List<VerificationEntity> entities = List.of(new VerificationEntity());
    when(verificationsRepository.findByUserId("user-1")).thenReturn(entities);

    List<VerificationEntity> result =
      verificationsService.getVerifications("user-1");

    assertEquals(1, result.size());
    verify(verificationsRepository).findByUserId("user-1");
  }

  @Test
  void getVerificationByIdShouldReturnEmptyWhenIdIsNull() {
    Optional<VerificationEntity> result =
      verificationsService.getVerificationById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getVerificationByIdShouldReturnEntityWhenFound() {
    VerificationEntity entity = new VerificationEntity();
    entity.setId("v1");
    entity.setUserId("user-1");
    when(verificationsRepository.findById("v1")).thenReturn(Optional.of(entity));

    Optional<VerificationEntity> result =
      verificationsService.getVerificationById("v1");

    assertTrue(result.isPresent());
    assertEquals("v1", result.get().getId());
    verify(verificationsRepository).findById("v1");
  }

  @Test
  void getVerificationByUserIdShouldReturnEmptyWhenUserIdIsNull() {
    Optional<VerificationEntity> result =
      verificationsService.getVerificationByUserId(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getVerificationByUserIdShouldReturnEntityWhenFound() {
    VerificationEntity entity = new VerificationEntity();
    entity.setUserId("user-1");
    when(verificationsRepository.findByUserId("user-1"))
      .thenReturn(List.of(entity));

    Optional<VerificationEntity> result =
      verificationsService.getVerificationByUserId("user-1");

    assertTrue(result.isPresent());
    assertEquals("user-1", result.get().getUserId());
    verify(verificationsRepository).findByUserId("user-1");
  }

  @Test
  void getVerificationByUserIdShouldReturnEmptyWhenNotFound() {
    when(verificationsRepository.findByUserId("missing"))
      .thenReturn(List.of());

    Optional<VerificationEntity> result =
      verificationsService.getVerificationByUserId("missing");

    assertFalse(result.isPresent());
    verify(verificationsRepository).findByUserId("missing");
  }

  @Test
  void updateVerificationShouldReturnEmptyWhenIdIsNull() {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("user-1");

    Optional<VerificationEntity> result =
      verificationsService.updateVerification(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateVerificationShouldReturnUpdatedEntityWhenFound() {
    VerificationEntity existing = new VerificationEntity();
    existing.setId("v1");
    existing.setUserId("user-1");
    VerificationDto dto = new VerificationDto();
    dto.setUserId("user-2");
    when(verificationsRepository.findById("v1"))
      .thenReturn(Optional.of(existing));
    when(verificationsRepository.save(any()))
      .thenAnswer(inv -> inv.getArgument(0));

    Optional<VerificationEntity> result =
      verificationsService.updateVerification("v1", dto);

    assertTrue(result.isPresent());
    verify(verificationsRepository).findById("v1");
    verify(verificationsRepository).save(any());
  }

  @Test
  void updateVerificationShouldReturnEmptyWhenNotFound() {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("user-1");
    when(verificationsRepository.findById("missing"))
      .thenReturn(Optional.empty());

    Optional<VerificationEntity> result =
      verificationsService.updateVerification("missing", dto);

    assertFalse(result.isPresent());
    verify(verificationsRepository).findById("missing");
  }

  @Test
  void deleteVerificationShouldReturnEmptyWhenIdIsNull() {
    Optional<VerificationEntity> result =
      verificationsService.deleteVerification(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteVerificationShouldReturnDeletedEntityWhenFound() {
    VerificationEntity entity = new VerificationEntity();
    entity.setId("v1");
    entity.setUserId("user-1");
    when(verificationsRepository.findById("v1"))
      .thenReturn(Optional.of(entity));

    Optional<VerificationEntity> result =
      verificationsService.deleteVerification("v1");

    assertTrue(result.isPresent());
    assertEquals("v1", result.get().getId());
    verify(verificationsRepository).findById("v1");
    verify(verificationsRepository).deleteById("v1");
  }

  @Test
  void deleteVerificationShouldReturnEmptyWhenNotFound() {
    when(verificationsRepository.findById("missing"))
      .thenReturn(Optional.empty());

    Optional<VerificationEntity> result =
      verificationsService.deleteVerification("missing");

    assertFalse(result.isPresent());
    verify(verificationsRepository).findById("missing");
  }

  @Test
  void getVerificationByUserIdShouldReturnEmptyWhenMultipleFound() {
    VerificationEntity first = new VerificationEntity();
    first.setId("v1");
    first.setUserId("user-1");
    VerificationEntity second = new VerificationEntity();
    second.setId("v2");
    second.setUserId("user-1");
    when(verificationsRepository.findByUserId("user-1"))
      .thenReturn(List.of(first, second));

    Optional<VerificationEntity> result =
      verificationsService.getVerificationByUserId("user-1");

    assertFalse(result.isPresent());
    verify(verificationsRepository).findByUserId("user-1");
  }

  @Test
  void createVerificationShouldPropagateExceptionWhenNonDuplicateKeyException() {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("user-1");
    when(verificationsRepository.insert(any(VerificationEntity.class)))
      .thenThrow(new RuntimeException("DB connection failed"));

    assertThrows(RuntimeException.class, () ->
      verificationsService.createVerification(dto));
    verify(verificationsRepository).insert(any(VerificationEntity.class));
  }
}
