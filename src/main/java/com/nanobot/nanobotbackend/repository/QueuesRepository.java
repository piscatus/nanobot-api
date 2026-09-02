package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QueuesRepository extends MongoRepository<QueueEntity, String> {
  @SuppressWarnings("null")
  List<QueueEntity> findAll();

  @Query(value = "{}", sort = "{ 'timestamp' : 1 }")
  List<QueueEntity> findAllSortedByOldestTimestamp();

  List<QueueEntity> findByTickerOrderByTimestampAsc(String ticker);

  boolean existsByTickerAndBlockHashAndLevel(
    String ticker,
    String blockHash,
    LevelDto level
  );

  boolean existsByTickerAndSourceHashAndLevel(
    String ticker,
    String sourceHash,
    LevelDto level
  );

  boolean existsByTickerAndSourceAddressAndLevel(
    String ticker,
    String sourceAddress,
    LevelDto level
  );

  Optional<QueueEntity> findByTickerAndBlockHashAndProcessed(
    String ticker,
    String blockHash,
    boolean processed
  );

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'userId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'ticker' : [1] } }" +
    "]}"
  )
  List<QueueEntity> findByParams(String userId, String ticker);
}
