package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CommandsService {
  Optional<CommandEntity> createCommand(CommandDto commandDto);

  List<CommandEntity> getCommands(String name);

  Optional<CommandEntity> getCommandById(String id);

  Optional<CommandEntity> getCommandByName(String name);

  Optional<CommandEntity> updateCommand(String id, CommandDto commandDto);

  List<CommandDto> updateCommands(List<CommandDto> commands);

  Optional<CommandEntity> deleteCommand(String id);

  boolean setCommands(
    String commandName,
    String userId,
    Consumer<List<CommandDto>> setCommands
  );
}
