package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.entity.DepositAddressEntity;
import java.util.Optional;

/**
 * Storage for deposit addresses that cannot be recomputed from a user's seed.
 * Only protocols that allocate addresses out of a shared wallet need this.
 */
public interface DepositAddressService {
  Optional<DepositAddressEntity> getByUser(String ticker, String userId);

  Optional<DepositAddressEntity> getByAddressIndex(
    String ticker,
    Long addressIndex
  );

  /**
   * The deposit address record for an address, if the bot issued it.
   *
   * <p>Used to recognise a withdrawal whose destination is one of our own
   * deposit addresses, so the recipient can be credited.
   */
  Optional<DepositAddressEntity> getByAddress(String ticker, String address);

  /**
   * Records an allocated address. Returns the stored entity, or the existing one
   * if a concurrent caller won the race, so an address is never issued twice for
   * the same user.
   */
  DepositAddressEntity record(
    String userId,
    String ticker,
    String address,
    Long addressIndex
  );
}
