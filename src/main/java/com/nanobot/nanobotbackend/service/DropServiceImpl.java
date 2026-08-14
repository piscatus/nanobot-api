package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.TimeUtil;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DropServiceImpl implements DropService {

  private final FileLogger fileLogger;

  private final CoreServices coreServices;

  private final DropsService dropsService;

  private final MessagesService messagesService;

  private final PickupsService pickupsService;

  @Autowired
  private TransferExecutorService transferExecutorService;

  public DropServiceImpl(
    CoreServices coreServices,
    DropsService dropsService,
    MessagesService messagesService,
    PickupsService pickupsService
  ) {
    this.fileLogger = new FileLogger("DropService");
    this.coreServices = coreServices;
    this.dropsService = dropsService;
    this.messagesService = messagesService;
    this.pickupsService = pickupsService;
  }

  public String formatPickeruppers(
    String dropCommandId,
    DropEntity drop,
    List<PickupDto> pickeruppers
  ) {
    int size = pickeruppers.size();
    String response =
      "<@" +
      drop.getUserId() +
      "> used </drop:" +
      dropCommandId +
      "> to transfer **" +
      drop.getInput() +
      "**\n" +
      drop.getMessageData() +
      " to ";

    if (size == 0) {
      response += "**no one**!";
      return response;
    }

    // Map all to formatted mentions
    List<String> mentions = pickeruppers
      .stream()
      .map(p -> "<@" + p.getUserId() + ">")
      .collect(Collectors.toList());

    int displayLimit = 40;

    // If more than 40, show first 40 and say "and X others!"
    if (size > displayLimit) {
      List<String> firstMentions = mentions.subList(0, displayLimit);
      String formatted = String.join(", ", firstMentions);
      int remaining = size - displayLimit;
      String othersText =
        " and " + remaining + (remaining == 1 ? " other!" : " others!");
      response += formatted + othersText;
      return response;
    }

    // 1 person
    if (size == 1) {
      response += mentions.get(0) + "!";
      return response;
    }

    // 2 people
    if (size == 2) {
      response += mentions.get(0) + " and " + mentions.get(1) + "!";
      return response;
    }

    // 3 to 40 people
    String allButLast = String.join(", ", mentions.subList(0, size - 1));
    String last = mentions.get(size - 1);
    response += allButLast + ", and " + last + "!";
    return response;
  }

  /**
   * Join order for end-of-drop messages: earliest pickup first (who joined first).
   * Selection logic may use a shuffled list; this is only for display.
   */
  private static List<PickupDto> sortPickupsByJoinTimeForDisplay(
    List<PickupDto> pickeruppers
  ) {
    List<PickupDto> sorted = new ArrayList<>(pickeruppers);
    sorted.sort(
      Comparator.comparing(
        PickupDto::getTimestamp,
        Comparator.nullsLast(Comparator.naturalOrder())
      )
    );
    return sorted;
  }

  @Override
  public BaseResponseDto dropUpdate(RequestDto requestDto) {
    String dropId = requestDto.getId();
    String messageId = requestDto.getDropId();

    if (
      dropId != null &&
      !dropId.equals("") &&
      !dropId.equals("0") &&
      messageId != null &&
      !messageId.equals("") &&
      !messageId.equals("0")
    ) {
      Optional<DropEntity> updatedDrop = dropsService.updateDropMessageId(
        requestDto
      );

      if (updatedDrop.isPresent()) {
        return new BaseResponseDto();
      }
    }
    return new BaseResponseDto("Drop failed to update.");
  }

  public void dropCleanup(DropEntity drop, List<PickupEntity> pickups) {
    fileLogger.info(
      "Cleaning up drop with ID: " +
      drop.getId() +
      " and pickups size: " +
      pickups.size()
    );
    String dropId = drop.getId();
    String userId = drop.getUserId();
    Optional<DropEntity> deletedDrop = dropsService.deleteDrop(dropId);
    if (deletedDrop.isPresent()) {
      TransferResponseDto transferResponseDto = new TransferResponseDto();
      coreServices.commandsService.setCommands(
        Constants.COMMAND_NAME_DROP,
        System.getenv("OWNER_USER_ID"),
        transferResponseDto::setCommands
      );

      Map<String, String> commandMap = transferResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      transferResponseDto.setPrimaryTransfer(drop.getTransfer());

      List<Map<String, Object>> criteriaList = new ArrayList<>();

      String valueField = "value";
      String nameField = "name";
      String inlineField = "inline";

      criteriaList.add(
        Map.of(
          nameField,
          "Drop Duration:",
          valueField,
          "**" +
          TimeUtil.formatTimeRemaining(drop.getDuration().longValue() * 60000) +
          "**",
          inlineField,
          true
        )
      );

      criteriaList.add(
        Map.of(
          nameField,
          "Drop Ended:",
          valueField,
          "<t:" + System.currentTimeMillis() / 1000 + ":f>",
          inlineField,
          true
        )
      );

      String maxEntries = drop.getMaximumEntries();
      if (
        maxEntries != null && !maxEntries.equals("0") && maxEntries.length() < 4
      ) {
        criteriaList.add(
          Map.of(
            nameField,
            "Maximum Entries:",
            valueField,
            "**" + maxEntries + "**",
            inlineField,
            true
          )
        );
      }

      Integer numberWinners = drop.getNumberWinners();
      if (numberWinners != null && numberWinners != 0) {
        criteriaList.add(
          Map.of(
            nameField,
            "Random Winners:",
            valueField,
            "**" + numberWinners + "**",
            inlineField,
            true
          )
        );
      }

      String requiredRole = drop.getRequiredRole();
      if (requiredRole != null && !requiredRole.equals("0")) {
        criteriaList.add(
          Map.of(
            nameField,
            "Required Role:",
            valueField,
            "<@&" + requiredRole + ">",
            inlineField,
            true
          )
        );
      }

      String grayColor = "#757575";

      MessageDto editMessage = null;
      if (pickups.isEmpty()) {
        criteriaList.add(
          Map.of("name", "Users Joined:", "value", "**0**", "inline", true)
        );

        transferExecutorService.executeTransfer(
          Constants.COMMAND_NAME_DROP,
          drop.getGuildId(),
          drop.getChannelId(),
          "0",
          null,
          Collections.singletonList(userId),
          null,
          transferResponseDto
        );

        editMessage = new MessageDto(
          null,
          drop.getGuildId(),
          drop.getChannelId(),
          drop.getMessageId(),
          "🏁 The drop has ended!",
          grayColor,
          formatPickeruppers(commandMap.get("drop"), drop, new ArrayList<>()),
          new Date(),
          null,
          criteriaList,
          null
        );
      } else {
        criteriaList.add(
          Map.of(
            nameField,
            "Users Joined:",
            valueField,
            "**" + pickups.size() + "**",
            inlineField,
            true
          )
        );
        int numberOfWinners = drop.getNumberWinners();
        List<PickupDto> pickeruppers = new ArrayList<>();

        if (numberOfWinners == 0) {
          for (PickupEntity pickup : pickups) {
            String pickupId = pickup.getId();
            Optional<PickupEntity> deletedPickup = pickupsService.deletePickup(
              pickupId
            );

            if (deletedPickup.isPresent()) {
              pickeruppers.add(new PickupDto(pickup));
            }
          }
        } else {
          Collections.shuffle(pickups, new SecureRandom());

          int count = 0;
          for (PickupEntity pickup : pickups) {
            String pickupId = pickup.getId();
            Optional<PickupEntity> deletedPickup = pickupsService.deletePickup(
              pickupId
            );

            if (deletedPickup.isPresent() && count < numberOfWinners) {
              pickeruppers.add(new PickupDto(pickup));
              count++;
            }
          }
        }

        transferResponseDto.setSecondaryTransfer(drop.getTransfer());

        transferExecutorService.executeTransfer(
          Constants.COMMAND_NAME_DROP,
          drop.getGuildId(),
          drop.getChannelId(),
          "0",
          userId,
          Collections.singletonList(userId),
          pickeruppers
            .stream()
            .map(PickupDto::getUserId)
            .collect(Collectors.toList()),
          transferResponseDto
        );

        editMessage = new MessageDto(
          null,
          drop.getGuildId(),
          drop.getChannelId(),
          drop.getMessageId(),
          "🏁 The drop has ended!",
          grayColor,
          formatPickeruppers(
            commandMap.get("drop"),
            drop,
            sortPickupsByJoinTimeForDisplay(pickeruppers)
          ),
          new Date(),
          null,
          criteriaList,
          null
        );
      }

      messagesService.createMessage(editMessage);
    }
  }

  @Override
  public void checkDropsByPickupSize() {
    List<DropEntity> drops = dropsService.getDrops(null);
    if (drops.isEmpty()) {
      return;
    }
    for (DropEntity drop : drops) {
      String maximumEntries = drop.getMaximumEntries();
      BigInteger bigMaximumEntries = new BigInteger(maximumEntries);
      if (drop.getMessageId() == null) {
        continue;
      }
      List<PickupEntity> pickups = drop.getMessageId() != null
        ? pickupsService.getPickups(drop.getMessageId(), null)
        : Collections.emptyList();
      if (BigInteger.valueOf(pickups.size()).compareTo(bigMaximumEntries) < 0) {
        continue;
      }
      dropCleanup(drop, pickups);
    }
  }

  @Override
  public void checkDropsByTime() {
    List<DropEntity> drops = dropsService.getDrops(null);
    if (drops.isEmpty()) {
      return;
    }
    for (DropEntity drop : drops) {
      if (drop.getEndTime().after(new Date())) {
        continue;
      }
      List<PickupEntity> pickups = drop.getMessageId() != null
        ? pickupsService.getPickups(drop.getMessageId(), null)
        : Collections.emptyList();

      dropCleanup(drop, pickups);
    }
  }
}
