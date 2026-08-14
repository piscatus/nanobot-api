package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.MessageEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessagesRepository
  extends MongoRepository<MessageEntity, String> {
  @SuppressWarnings("null")
  List<MessageEntity> findAll();

  @Query(value = "{}", sort = "{ 'timestamp' : 1 }")
  List<MessageEntity> findAllSortedByOldestTimestamp();

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'userId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'messageId' : [1] } }" +
    "]}"
  )
  List<MessageEntity> findByParams(String userId, String messageId);
}
