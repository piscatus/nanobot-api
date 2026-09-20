package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.StringUtil;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DropServiceImpl implements DropService {

  private final FileLogger fileLogger;

  private final CoreServices coreServices;

  private final CurrenciesService currenciesService;

  private final DropsService dropsService;

  private final MessagesService messagesService;

  private final PickupsService pickupsService;

  @Autowired
  private TransferExecutorService transferExecutorService;

  public DropServiceImpl(
    CoreServices coreServices,
    CurrenciesService currenciesService,
    DropsService dropsService,
    MessagesService messagesService,
    PickupsService pickupsService
  ) {
    this.fileLogger = new FileLogger("DropService");
    this.coreServices = coreServices;
    this.currenciesService = currenciesService;
    this.dropsService = dropsService;
    this.messagesService = messagesService;
    this.pickupsService = pickupsService;
  }

  public String formatPickeruppers(
    String dropCommandId,
    DropEntity drop,
    List<PickupDto> pickeruppers
  ) {
    return formatPickeruppers(dropCommandId, drop, pickeruppers, null);
  }

  /**
   * @param eachSummary per-recipient amount, appended so winners can see what
   *   they individually received rather than only the pooled total. Null when
   *   the split is not known, such as a drop that ended with no winners.
   */
  public String formatPickeruppers(
    String dropCommandId,
    DropEntity drop,
    List<PickupDto> pickeruppers,
    String eachSummary
  ) {
    return formatPickeruppers(
      dropCommandId,
      drop,
      pickeruppers,
      eachSummary,
      Constants.COMMAND_NAME_DROP
    );
  }

  /**
   * @param commandName which slash command created the drop, so the closing
   *   embed links the right one ("drop" or "triviadrop").
   */
  public String formatPickeruppers(
    String dropCommandId,
    DropEntity drop,
    List<PickupDto> pickeruppers,
    String eachSummary,
    String commandName
  ) {
    int size = pickeruppers.size();
    // Mention plus plain username: an uncached member renders as a raw id, so
    // the mention alone leaves other readers unable to tell who dropped.
    String creator =
      "<@" +
      drop.getUserId() +
      ">" +
      (StringUtil.isValidString(drop.getUsername())
          ? " (" + drop.getUsername() + ")"
          : "");

    String response =
      creator +
      " used </" +
      commandName +
      ":" +
      dropCommandId +
      "> to transfer **" +
      drop.getInput() +
      "**\n" +
      drop.getMessageData() +
      " to ";

    if (size == 0) {
      return response + "**no one**!";
    }

    response += formatRecipientMentions(pickeruppers);

    // Appended after the recipient list so every winner count gets it.
    if (StringUtil.isValidString(eachSummary)) {
      response += "\n" + eachSummary;
    }
    return response;
  }

  private static String formatRecipientMentions(List<PickupDto> pickeruppers) {
    int size = pickeruppers.size();

    List<String> mentions = pickeruppers
      .stream()
      .map(p -> "<@" + p.getUserId() + ">")
      .collect(Collectors.toList());

    int displayLimit = 40;

    // If more than 40, show first 40 and say "and X others!"
    if (size > displayLimit) {
      String formatted = String.join(
        ", ",
        mentions.subList(0, displayLimit)
      );
      int remaining = size - displayLimit;
      return (
        formatted +
        " and " +
        remaining +
        (remaining == 1 ? " other!" : " others!")
      );
    }

    if (size == 1) {
      return mentions.get(0) + "!";
    }

    if (size == 2) {
      return mentions.get(0) + " and " + mentions.get(1) + "!";
    }

    // 3 to 40 people
    String allButLast = String.join(", ", mentions.subList(0, size - 1));
    return allButLast + ", and " + mentions.get(size - 1) + "!";
  }

  /**
   * Compact per-recipient summary built from what was actually transferred,
   * rather than dividing the pooled input, so integer-division remainders are
   * reflected accurately.
   */
  private String formatEachSummary(TransferResponseDto transfer) {
    if (transfer == null || transfer.getCompletedSecondaryTransfers() == null) {
      return null;
    }
    Optional<TransferDto> perRecipient = transfer
      .getCompletedSecondaryTransfers()
      .flatMap(map -> map.values().stream().findFirst());
    if (perRecipient.isEmpty()) {
      return null;
    }

    List<String> parts = new ArrayList<>();

    for (WalletDto wallet : perRecipient.get().getWallets()) {
      if (wallet.getRaw() == null || "0".equals(wallet.getRaw())) {
        continue;
      }
      Optional<CurrencyEntity> currency = currenciesService.getCurrencyByTicker(
        wallet.getTicker()
      );
      if (currency.isEmpty()) {
        continue;
      }
      parts.add(
        "**" +
        currenciesService.getCurrencyDecimalValue(
          wallet.getRaw(),
          Integer.parseInt(currency.get().getPrecision())
        ) +
        " " +
        wallet.getTicker() +
        "** " +
        currency.get().getEmoji()
      );
    }

    for (ItemDto item : perRecipient.get().getItems()) {
      if (item.getQuantity() <= 0) {
        continue;
      }
      parts.add("**" + item.getQuantity() + " " + item.getName() + "**");
    }

    if (parts.isEmpty()) {
      return null;
    }
    return "-# Each winner received " + String.join(", ", parts) + ".";
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

  /** Pickups on a trivia drop that pressed the right button. */
  static List<PickupEntity> correctPickups(
    DropEntity drop,
    List<PickupEntity> pickups
  ) {
    if (!drop.hasTrivia()) {
      return new ArrayList<>(pickups);
    }
    return pickups
      .stream()
      .filter(p -> drop.getTrivia().isCorrect(p.getAnswerIndex()))
      .collect(Collectors.toList());
  }

  /**
   * Who a trivia drop pays: the correct answers in the order they arrived,
   * capped at maximumEntries. Speed breaks the tie, not chance, and the cap
   * re-applies here so two answers landing in the same instant cannot pay one
   * winner more than the drop allows.
   */
  static List<PickupEntity> selectTriviaWinners(
    DropEntity drop,
    List<PickupEntity> pickups
  ) {
    List<PickupEntity> correct = correctPickups(drop, pickups);
    correct.sort(
      Comparator.comparing(
        PickupEntity::getTimestamp,
        Comparator.nullsLast(Comparator.naturalOrder())
      )
    );
    BigInteger maximum = drop.getMaximumEntries() == null
      ? null
      : new BigInteger(drop.getMaximumEntries());
    if (
      maximum != null &&
      maximum.signum() > 0 &&
      maximum.compareTo(BigInteger.valueOf(correct.size())) < 0
    ) {
      return new ArrayList<>(correct.subList(0, maximum.intValue()));
    }
    return correct;
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
    boolean trivia = drop.hasTrivia();
    String commandName = trivia
      ? Constants.COMMAND_NAME_TRIVIADROP
      : Constants.COMMAND_NAME_DROP;
    String endedTitle = trivia
      ? "🏁 The trivia drop has ended!"
      : "🏁 The drop has ended!";
    Optional<DropEntity> deletedDrop = dropsService.deleteDrop(dropId);
    if (deletedDrop.isPresent()) {
      TransferResponseDto transferResponseDto = new TransferResponseDto();
      coreServices.commandsService.setCommands(
        commandName,
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
          TimeUtil.formatTimeRemaining(dropDurationMillis(drop)) +
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
            trivia ? "Maximum Winners:" : "Maximum Entries:",
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

      // A trivia drop reveals its question and answer once it is over, and
      // pays only the correct answers; a plain drop considers every joiner.
      List<PickupEntity> winners = trivia
        ? selectTriviaWinners(drop, pickups)
        : pickups;
      if (trivia) {
        criteriaList.add(
          Map.of(
            nameField,
            "Question:",
            valueField,
            drop.getTrivia().getQuestion(),
            inlineField,
            false
          )
        );
        criteriaList.add(
          Map.of(
            nameField,
            "Correct Answer:",
            valueField,
            "**" +
            Optional
              .ofNullable(drop.getTrivia().getCorrectAnswer())
              .orElse("?") +
            "**",
            inlineField,
            false
          )
        );
        criteriaList.add(
          Map.of(
            nameField,
            "Answered:",
            valueField,
            "**" +
            pickups.size() +
            "** (**" +
            correctPickups(drop, pickups).size() +
            "** correct)",
            inlineField,
            true
          )
        );
      }

      MessageDto editMessage = null;
      if (winners.isEmpty()) {
        if (!trivia) {
          criteriaList.add(
            Map.of("name", "Users Joined:", "value", "**0**", "inline", true)
          );
        }
        // Wrong answers on a trivia drop still have to be cleared away.
        for (PickupEntity pickup : pickups) {
          pickupsService.deletePickup(pickup.getId());
        }

        transferExecutorService.executeTransfer(
          commandName,
          drop.getGuildId(),
          drop.getChannelId(),
          "0",
          null,
          Collections.singletonList(userId),
          null,
          transferResponseDto
        );

        String content = formatPickeruppers(
          commandMap.get(commandName),
          drop,
          new ArrayList<>(),
          null,
          commandName
        );
        if (trivia) {
          content +=
            "\n-# Nobody answered correctly, so the drop was returned.";
        }

        editMessage = new MessageDto(
          null,
          drop.getGuildId(),
          drop.getChannelId(),
          drop.getMessageId(),
          endedTitle,
          grayColor,
          content,
          new Date(),
          null,
          criteriaList,
          null
        );
      } else {
        if (!trivia) {
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
        }
        int numberOfWinners = drop.getNumberWinners();
        List<PickupDto> pickeruppers = new ArrayList<>();

        if (trivia) {
          // Every answer is cleared; only the selected winners are paid.
          Set<String> winnerIds = winners
            .stream()
            .map(PickupEntity::getId)
            .collect(Collectors.toSet());
          for (PickupEntity pickup : pickups) {
            Optional<PickupEntity> deletedPickup = pickupsService.deletePickup(
              pickup.getId()
            );
            if (deletedPickup.isPresent() && winnerIds.contains(pickup.getId())) {
              pickeruppers.add(new PickupDto(pickup));
            }
          }
        } else if (numberOfWinners == 0) {
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

        TransferResponseDto completed = transferExecutorService.executeTransfer(
          commandName,
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
          endedTitle,
          grayColor,
          formatPickeruppers(
            commandMap.get(commandName),
            drop,
            sortPickupsByJoinTimeForDisplay(pickeruppers),
            formatEachSummary(completed),
            commandName
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
      // On a trivia drop only correct answers count towards the cap.
      int counted = drop.hasTrivia()
        ? correctPickups(drop, pickups).size()
        : pickups.size();
      if (BigInteger.valueOf(counted).compareTo(bigMaximumEntries) < 0) {
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

  private static long dropDurationMillis(DropEntity drop) {
    long minutes = drop.getDuration() == null ? 0L : drop.getDuration();
    long seconds = drop.getSeconds() == null ? 0L : drop.getSeconds();
    return minutes * 60000L + seconds * 1000L;
  }
}
