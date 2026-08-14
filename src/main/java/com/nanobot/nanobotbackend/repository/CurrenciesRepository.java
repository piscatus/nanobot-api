package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrenciesRepository
  extends MongoRepository<CurrencyEntity, String> {
  @SuppressWarnings("null")
  List<CurrencyEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'ticker' : [0] } }")
  List<CurrencyEntity> findByTicker(String ticker);
}
