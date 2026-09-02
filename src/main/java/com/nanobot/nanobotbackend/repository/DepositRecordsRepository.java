package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.DepositRecordEntity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositRecordsRepository
  extends MongoRepository<DepositRecordEntity, String> {
  Optional<DepositRecordEntity> findByTickerAndTxidAndAddressIndex(
    String ticker,
    String txid,
    Long addressIndex
  );

  boolean existsByTickerAndTxidAndAddressIndex(
    String ticker,
    String txid,
    Long addressIndex
  );
}
