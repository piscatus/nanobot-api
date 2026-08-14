package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.TransactionEntity;
import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionsRepository
  extends MongoRepository<TransactionEntity, String> {
  List<TransactionEntity> findAllByOrderByTimestampDesc();

  @Query(
    value = "{ '$or': [ " +
    "  { 'primaryUserId': ?0 }, " +
    "  { 'secondaryUserId': ?0 }, " +
    "  { 'primaryReceiverIds': ?0 }, " +
    "  { 'secondaryReceiverIds': ?0 } " +
    "] }",
    sort = "{ 'timestamp': -1 }"
  )
  List<TransactionEntity> findByAnyUserId(String userId);

  long deleteByTimestampBefore(Date timestamp);
}
