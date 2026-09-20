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
      boolean trivia = drop.hasTrivia();
      String noun = trivia ? "trivia drop" : "drop";
      if (drop.getEndTime().before(new java.util.Date())) {
        baseResponseDto.setErrorMessage("This " + noun + " has ended.");
        return baseResponseDto;
      }
      List<PickupEntity> pickups = pickupsService.getPickups(
        requestDto.getDropId(),
        null
      );
      // A trivia drop reports how many have answered, never how many were right.
      String usersJoinedNote = trivia
        ? "\n\nAnswers: " + pickups.size()
        : "\n\nUsers Joined: " + pickups.size();
      if (drop.getUserId().equals(requestDto.getUserId())) {
        baseResponseDto.setErrorMessage(
          (trivia
              ? "You cannot answer your own trivia drop!"
              : "You cannot enter your own drop!") +
          usersJoinedNote
        );
        return baseResponseDto;
      }
      for (PickupEntity pickup : pickups) {
        if (pickup.getUserId().equals(requestDto.getUserId())) {
          baseResponseDto.setErrorMessage(
            (trivia
                ? "You have already answered this trivia drop!"
                : "You have already entered this drop!") +
            usersJoinedNote
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
      Integer answerIndex = null;
      if (trivia) {
        // Anyone may answer; maximumEntries caps winners, checked below.
        answerIndex = requestDto.getAnswerIndex();
        List<String> answers = drop.getTrivia().getAnswers();
        if (
          answerIndex == null ||
          answers == null ||
          answerIndex < 0 ||
          answerIndex >= answers.size()
        ) {
          baseResponseDto.setErrorMessage(
            "Please choose one of the answer buttons." + usersJoinedNote
          );
          return baseResponseDto;
        }
      } else if (currentSize.compareTo(bigInt) >= 0) {
        baseResponseDto.setErrorMessage(
          "This drop has reached its maximum number of entries."
        );
        return baseResponseDto;
      }
      PickupDto pickupDto = new PickupDto(
        null,
        requestDto.getDropId(),
        requestDto.getUserId(),
        new java.util.Date(),
        answerIndex
      );
      Optional<PickupEntity> createdPickup = pickupsService.createPickup(
        pickupDto
      );
      if (createdPickup.isEmpty()) {
        baseResponseDto.setErrorMessage(
          trivia
            ? "Error recording your answer, please try again later."
            : "Error joining drop, please try again later."
        );
        return baseResponseDto;
      }
      if (trivia) {
        // The winner slots are full once enough correct answers are in, so end
        // the drop now rather than making everyone wait out the timer. Nothing
        // here tells the answerer whether they were one of them.
        long correct = pickups
          .stream()
          .filter(p -> drop.getTrivia().isCorrect(p.getAnswerIndex()))
          .count();
        if (drop.getTrivia().isCorrect(answerIndex)) {
          correct++;
        }
        if (BigInteger.valueOf(correct).compareTo(bigInt) >= 0) {
          drop.setEndTime(new Date());
          dropsService.updateDrop(drop.getId(), new DropDto(drop));
        }
      } else if (bigInt.subtract(currentSize).equals(BigInteger.ONE)) {
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
