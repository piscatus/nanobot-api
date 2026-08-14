package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.DropEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DropsRepository extends MongoRepository<DropEntity, String> {
  @SuppressWarnings("null")
  List<DropEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'messageId' : [0] } }")
  List<DropEntity> findByMessageId(String messageId);
}
