package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserItemsRepository
  extends MongoRepository<UserItemsEntity, String> {
  @SuppressWarnings("null")
  List<UserItemsEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'userId' : [0] } }")
  List<UserItemsEntity> findByUserId(String userId);
}
