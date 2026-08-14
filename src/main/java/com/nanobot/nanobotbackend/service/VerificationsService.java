package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.VerificationDto;
import com.nanobot.nanobotbackend.entity.VerificationEntity;
import java.util.List;
import java.util.Optional;

public interface VerificationsService {
  Optional<VerificationEntity> createVerification(VerificationDto verificationDto);

  List<VerificationEntity> getVerifications(String userId);

  Optional<VerificationEntity> getVerificationById(String id);

  Optional<VerificationEntity> getVerificationByUserId(String userId);

  Optional<VerificationEntity> updateVerification(
    String id,
    VerificationDto verificationDto
  );

  Optional<VerificationEntity> deleteVerification(String id);
}
