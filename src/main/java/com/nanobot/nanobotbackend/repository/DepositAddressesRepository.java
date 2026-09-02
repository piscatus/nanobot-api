package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.DepositAddressEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepositAddressesRepository
  extends MongoRepository<DepositAddressEntity, String> {
  Optional<DepositAddressEntity> findByTickerAndUserId(
    String ticker,
    String userId
  );

  Optional<DepositAddressEntity> findByTickerAndAddressIndex(
    String ticker,
    Long addressIndex
  );

  Optional<DepositAddressEntity> findByTickerAndAddress(
    String ticker,
    String address
  );

  List<DepositAddressEntity> findByTicker(String ticker);
}
