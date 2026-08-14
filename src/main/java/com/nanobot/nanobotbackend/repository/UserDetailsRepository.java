package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDetailsRepository
  extends MongoRepository<UserDetailsEntity, String> {
  @SuppressWarnings("null")
  List<UserDetailsEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'userId' : [0] } }")
  List<UserDetailsEntity> findByUserId(String userId);

  @Query("{ status: 'ACTIVE' }")
  List<UserDetailsEntity> findActiveUsers();

  @Query("{ status: 'LOCKED' }")
  List<UserDetailsEntity> findLockedUsers();

  @Query("{ status: 'BANNED' }")
  List<UserDetailsEntity> findBannedUsers();
}
