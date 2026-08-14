package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.VerificationEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationsRepository
  extends MongoRepository<VerificationEntity, String> {
  @SuppressWarnings("null")
  List<VerificationEntity> findAll();

  List<VerificationEntity> findByUserId(String userId);
}
