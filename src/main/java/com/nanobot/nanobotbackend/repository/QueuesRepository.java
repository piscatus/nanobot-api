package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QueuesRepository extends MongoRepository<QueueEntity, String> {
  @SuppressWarnings("null")
  List<QueueEntity> findAll();

  @Query(value = "{}", sort = "{ 'timestamp' : 1 }")
  List<QueueEntity> findAllSortedByOldestTimestamp();

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'userId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'ticker' : [1] } }" +
    "]}"
  )
  List<QueueEntity> findByParams(String userId, String ticker);
}
