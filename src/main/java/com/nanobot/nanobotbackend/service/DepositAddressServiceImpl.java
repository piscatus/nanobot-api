package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.entity.DepositAddressEntity;
import com.nanobot.nanobotbackend.repository.DepositAddressesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class DepositAddressServiceImpl implements DepositAddressService {

  private final DepositAddressesRepository depositAddressesRepository;

  private final FileLogger fileLogger;

  public DepositAddressServiceImpl(
    DepositAddressesRepository depositAddressesRepository
  ) {
    this.depositAddressesRepository = depositAddressesRepository;
    this.fileLogger = new FileLogger("DepositAddressService");
  }

  @Override
  public Optional<DepositAddressEntity> getByUser(
    String ticker,
    String userId
  ) {
    if (ticker == null || userId == null) {
      return Optional.empty();
    }
    return depositAddressesRepository.findByTickerAndUserId(ticker, userId);
  }

  @Override
  public Optional<DepositAddressEntity> getByAddressIndex(
    String ticker,
    Long addressIndex
  ) {
    if (ticker == null || addressIndex == null) {
      return Optional.empty();
    }
    return depositAddressesRepository.findByTickerAndAddressIndex(
      ticker,
      addressIndex
    );
  }

  @Override
  public Optional<DepositAddressEntity> getByAddress(
    String ticker,
    String address
  ) {
    if (ticker == null || address == null) {
      return Optional.empty();
    }
    return depositAddressesRepository.findByTickerAndAddress(ticker, address);
  }

  @Override
  public DepositAddressEntity record(
    String userId,
    String ticker,
    String address,
    Long addressIndex
  ) {
    DepositAddressEntity entity = new DepositAddressEntity(
      userId,
      ticker,
      address,
      addressIndex
    );
    entity.setId(new ObjectId().toHexString());
    try {
      DepositAddressEntity saved = depositAddressesRepository.insert(entity);
      fileLogger.info(
        "Allocated " +
        ticker +
        " deposit address index " +
        addressIndex +
        " to user " +
        userId
      );
      return saved;
    } catch (DuplicateKeyException e) {
      // Another request allocated first. Reuse that address rather than
      // handing this user a second one, which would orphan the loser's funds.
      return depositAddressesRepository
        .findByTickerAndUserId(ticker, userId)
        .orElseThrow(() -> e);
    }
  }
}
