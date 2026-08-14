package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import java.util.List;

public interface TransferExecutorService {
  TransferResponseDto executeTransfer(
    String command,
    String guildId,
    String channelId,
    String primaryUserId,
    String secondaryUserId,
    List<String> primaryReceiverIds,
    List<String> secondaryReceiverIds,
    TransferResponseDto transferResponseDto
  );
}
