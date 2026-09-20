package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.DepositNoticeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositNoticesRepository
  extends MongoRepository<DepositNoticeEntity, String> {
  boolean existsByTickerAndTxidAndAddressIndex(
    String ticker,
    String txid,
    Long addressIndex
  );
}
