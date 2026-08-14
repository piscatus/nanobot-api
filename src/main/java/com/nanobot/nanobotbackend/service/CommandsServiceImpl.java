package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.repository.CommandsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CommandsServiceImpl implements CommandsService {

  private CommandsRepository commandsRepository;

  private final FileLogger fileLogger;

  public CommandsServiceImpl(CommandsRepository commandsRepository) {
    this.fileLogger = new FileLogger("CommandsService");
    this.commandsRepository = commandsRepository;
  }

  @Override
  public boolean setCommands(
    String commandName,
    String userId,
    Consumer<List<CommandDto>> setCommands
  ) {
    boolean isOwner = userId.equals(System.getenv("OWNER_USER_ID"));
    if (StringUtil.isValidString(commandName)) {
      List<CommandEntity> entities = getCommands(null);
      List<CommandDto> commands = new ArrayList<>();
      for (CommandEntity commandEntity : entities) {
        CommandDto commandDto = new CommandDto(commandEntity);
        if (
          !isOwner &&
          commandName.equals(commandEntity.getName()) &&
          commandEntity.getStatus() != StatusDto.ACTIVE
        ) {
          commands.add(commandDto);
          setCommands.accept(commands);
          return false;
        }
        commands.add(commandDto);
      }
      setCommands.accept(commands);
      return true;
    }
    return false;
  }

  @Override
  public Optional<CommandEntity> createCommand(CommandDto dto) {
    CommandEntity entity = new CommandEntity(dto);
    ObjectId id = new ObjectId();
    entity.setId(id.toHexString());
    fileLogger.info("Creating command with name: " + entity.getName());
    try {
      CommandEntity createdCommand = commandsRepository.insert(entity);
      fileLogger.info("Command created with ID: " + createdCommand.getId());
      return Optional.of(createdCommand);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Command already exists with specified name: " + dto.getName()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating command: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<CommandEntity> getCommands(String name) {
    try {
      if (name == null) {
        // fileLogger.info("Fetching all commands.");
        return commandsRepository.findAll();
      }
      fileLogger.info("Fetching commands with name: " + name);
      return commandsRepository.findByName(name);
    } catch (Exception e) {
      fileLogger.error("Error fetching commands: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<CommandEntity> getCommandById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching command with ID: " + id);
        return commandsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching command by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CommandEntity> getCommandByName(String name) {
    if (name != null) {
      try {
        fileLogger.info("Fetching command with name: " + name);
        List<CommandEntity> existingCommands = commandsRepository.findByName(
          name
        );
        if (existingCommands.size() > 1) {
          fileLogger.error(
            "Multiple commands found with the same name: " + name
          );
        } else if (!existingCommands.isEmpty()) {
          return Optional.of(existingCommands.get(0));
        }
      } catch (Exception e) {
        fileLogger.error("Error fetching command by name: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CommandEntity> updateCommand(String id, CommandDto dto) {
    if (id != null) {
      try {
        Optional<CommandEntity> commandOptional = commandsRepository.findById(
          id
        );
        if (commandOptional.isPresent()) {
          CommandEntity command = commandOptional.get();
          command.setName(dto.getName());
          command.setCommandId(dto.getCommandId());
          command.setStatus(dto.getStatus());

          CommandEntity updatedCommand = commandsRepository.save(command);
          fileLogger.info("Command updated with ID: " + id);
          return Optional.of(updatedCommand);
        } else {
          fileLogger.warn("Command not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating command: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CommandEntity> deleteCommand(String id) {
    if (id != null) {
      Optional<CommandEntity> commandOptional = commandsRepository.findById(id);
      if (commandOptional.isPresent()) {
        CommandEntity entity = commandOptional.get();
        commandsRepository.deleteById(id);
        fileLogger.info("Command deleted with ID: " + id);
        return Optional.of(entity);
      } else {
        fileLogger.warn("Command not found with ID: " + id);
      }
    }
    return Optional.empty();
  }

  @Override
  public List<CommandDto> updateCommands(List<CommandDto> commands) {
    if (commands == null || commands.isEmpty()) {
      return Collections.emptyList();
    }

    List<CommandEntity> currentCommands = commandsRepository.findAll();

    Map<String, CommandEntity> commandMap = currentCommands
      .stream()
      .collect(Collectors.toMap(CommandEntity::getName, command -> command));

    List<CommandDto> updatedCommands = new ArrayList<>();

    for (CommandDto command : commands) {
      CommandEntity existing = commandMap.get(command.getName());

      if (existing != null) {
        if (!command.getCommandId().equals(existing.getCommandId())) {
          command.setStatus(existing.getStatus());
          updateCommand(existing.getId(), command);
        } else {
          command.setStatus(existing.getStatus());
        }
      } else {
        command.setStatus(StatusDto.LOCKED);
        Optional<CommandEntity> created = createCommand(command);
        created.ifPresent(entity -> command.setCommandId(entity.getCommandId())
        );
      }

      updatedCommands.add(command);
    }

    return updatedCommands;
  }
}
