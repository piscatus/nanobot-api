package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.VerificationDto;
import com.nanobot.nanobotbackend.entity.VerificationEntity;
import com.nanobot.nanobotbackend.repository.VerificationsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class VerificationsServiceImpl implements VerificationsService {

  private VerificationsRepository verificationsRepository;

  private final FileLogger fileLogger;

  private static final String UPDATE_BY_ID = "Verification updated with ID: ";

  private static final String NOT_FOUND_BY_ID = "Verification not found with ID: ";

  public VerificationsServiceImpl(VerificationsRepository verificationsRepository) {
    this.fileLogger = new FileLogger("VerificationsService");
    this.verificationsRepository = verificationsRepository;
  }

  @Override
  public Optional<VerificationEntity> createVerification(
    VerificationDto verificationDto
  ) {
    VerificationEntity verificationEntity = new VerificationEntity(
      verificationDto
    );
    ObjectId id = new ObjectId();
    verificationEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating verification with userId: " + verificationEntity.getUserId()
    );
    try {
      VerificationEntity created =
        verificationsRepository.insert(verificationEntity);
      fileLogger.info("Verification created with ID: " + created.getId());
      return Optional.of(created);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Verification already exists with specified userId: " +
        verificationDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating verification: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<VerificationEntity> getVerifications(String userId) {
    try {
      if (userId == null) {
        fileLogger.info("Fetching all verifications.");
        return verificationsRepository.findAll();
      }
      fileLogger.info("Fetching verifications with userId: " + userId);
      return verificationsRepository.findByUserId(userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching verifications: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<VerificationEntity> getVerificationById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching verification with ID: " + id);
        return verificationsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching verification by ID: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<VerificationEntity> getVerificationByUserId(String userId) {
    if (userId != null) {
      try {
        fileLogger.info(
          "Fetching verification with userId: " + userId
        );
        List<VerificationEntity> existing =
          verificationsRepository.findByUserId(userId);
        if (existing.size() > 1) {
          fileLogger.error(
            "Multiple verifications found with the same userId: " + userId
          );
        } else if (!existing.isEmpty()) {
          return Optional.of(existing.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching verification by userId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<VerificationEntity> updateVerification(
    String id,
    VerificationDto verificationDto
  ) {
    if (id != null) {
      try {
        Optional<VerificationEntity> optional =
          verificationsRepository.findById(id);
        if (optional.isPresent()) {
          VerificationEntity entity = optional.get();
          entity.setUserId(verificationDto.getUserId());
          entity.setTimestamp(new Date());

          VerificationEntity updated = verificationsRepository.save(entity);
          fileLogger.info(UPDATE_BY_ID + id);
          return Optional.of(updated);
        } else {
          fileLogger.warn(NOT_FOUND_BY_ID + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating verification: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<VerificationEntity> deleteVerification(String id) {
    if (id != null) {
      Optional<VerificationEntity> optional =
        verificationsRepository.findById(id);

      if (optional.isPresent()) {
        VerificationEntity entity = optional.get();
        verificationsRepository.deleteById(id);
        fileLogger.info("Verification deleted with ID: " + id);
        return Optional.of(entity);
      } else {
        fileLogger.warn(NOT_FOUND_BY_ID + id);
      }
    }
    return Optional.empty();
  }
}
