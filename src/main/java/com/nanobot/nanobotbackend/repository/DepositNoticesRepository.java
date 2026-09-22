package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.DepositNoticeEntity;
import java.util.List;
import java.util.Optional;
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

  Optional<DepositNoticeEntity> findByTickerAndTxidAndAddressIndex(
    String ticker,
    String txid,
    Long addressIndex
  );

  List<DepositNoticeEntity> findByTickerAndConfirmedAtIsNull(String ticker);
}
