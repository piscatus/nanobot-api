package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PickupServicesImpl implements PickupServices {

  public final CoreServices coreServices;

  public final DropsService dropsService;

  public final PickupsService pickupsService;

  public PickupServicesImpl(
    CoreServices coreServices,
    DropsService dropsService,
    PickupsService pickupsService
  ) {
    this.coreServices = coreServices;
    this.dropsService = dropsService;
    this.pickupsService = pickupsService;
  }

  @Override
  public BaseResponseDto pickup(RequestDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_PICKUP, requestDto);

      BaseResponseDto baseResponseDto = new BaseResponseDto();

      List<DropEntity> drops = dropsService.getDrops(requestDto.getDropId());
      if (drops.isEmpty()) {
        baseResponseDto.setErrorMessage("Drop not found.");
        return baseResponseDto;
      }
      DropEntity drop = drops.get(0);
      if (drop.getEndTime().before(new java.util.Date())) {
        baseResponseDto.setErrorMessage("This drop has ended.");
        return baseResponseDto;
      }
      List<PickupEntity> pickups = pickupsService.getPickups(
        requestDto.getDropId(),
        null
      );
      String usersJoinedNote = "\n\nUsers Joined: " + pickups.size();
      if (drop.getUserId().equals(requestDto.getUserId())) {
        baseResponseDto.setErrorMessage(
          "You cannot enter your own drop!" + usersJoinedNote
        );
        return baseResponseDto;
      }
      for (PickupEntity pickup : pickups) {
        if (pickup.getUserId().equals(requestDto.getUserId())) {
          baseResponseDto.setErrorMessage(
            "You have already entered this drop!" + usersJoinedNote
          );
          return baseResponseDto;
        }
      }
      if (
        !drop.getRequiredRole().equals("0") &&
        !requestDto.getUserRoles().contains(drop.getRequiredRole())
      ) {
        baseResponseDto.setErrorMessage(
          "You do not hold the <@&" +
          drop.getRequiredRole() +
          "> role required to enter, sorry!" +
          usersJoinedNote
        );
        return baseResponseDto;
      }
      String maximumEntries = drop.getMaximumEntries();
      BigInteger bigInt = new BigInteger(maximumEntries);
      BigInteger currentSize = BigInteger.valueOf(pickups.size());
      if (currentSize.compareTo(bigInt) >= 0) {
        baseResponseDto.setErrorMessage(
          "This drop has reached its maximum number of entries."
        );
        return baseResponseDto;
      }
      PickupDto pickupDto = new PickupDto(
        null,
        requestDto.getDropId(),
        requestDto.getUserId(),
        new java.util.Date()
      );
      Optional<PickupEntity> createdPickup = pickupsService.createPickup(
        pickupDto
      );
      if (createdPickup.isEmpty()) {
        baseResponseDto.setErrorMessage(
          "Error joining drop, please try again later."
        );
        return baseResponseDto;
      }
      if (bigInt.subtract(currentSize).equals(BigInteger.ONE)) {
        drop.setEndTime(new Date());
        dropsService.updateDrop(drop.getId(), new DropDto(drop));
      }
      return baseResponseDto;
    } catch (Exception e) {
      System.out.println(e);
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_PICKUP, e);
      return new BaseResponseDto(Constants.unknownError);
    }
  }
}
