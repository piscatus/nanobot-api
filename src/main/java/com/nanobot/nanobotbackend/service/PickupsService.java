package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import java.util.List;
import java.util.Optional;

public interface PickupsService {
  Optional<PickupEntity> createPickup(PickupDto pickupDto);

  List<PickupEntity> getPickups(String dropId, String userId);

  Optional<PickupEntity> getPickupById(String id);

  Optional<PickupEntity> getPickupByDropIdAndUserId(
    String dropId,
    String userId
  );

  Optional<PickupEntity> updatePickup(String id, PickupDto pickupDto);

  Optional<PickupEntity> deletePickup(String id);
}
