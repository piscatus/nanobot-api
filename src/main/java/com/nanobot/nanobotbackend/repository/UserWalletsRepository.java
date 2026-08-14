package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWalletsRepository
  extends MongoRepository<UserWalletsEntity, String> {
  @SuppressWarnings("null")
  List<UserWalletsEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'userId' : [0] } }")
  List<UserWalletsEntity> findByUserId(String userId);
}
