package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface TransferService {
  public TransferDto processInputs(
    String command,
    Consumer<String> setErrorMessage,
    Map<String, String> commandMap,
    String guildId,
    String userId,
    String inputs,
    boolean checkForPricey
  );

  public TransferResponseDto transfer(
    String guildId,
    String userId,
    List<String> receiverIds,
    TransferResponseDto transferResponseDto,
    boolean isPrimary
  );
}
