package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.repository.CommandsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class CommandsServiceImplTest {

  @Mock
  private CommandsRepository commandsRepository;

  private CommandsServiceImpl commandsService;

  @BeforeEach
  void setUp() {
    commandsService = new CommandsServiceImpl(commandsRepository);
  }

  @Test
  void createCommandShouldReturnCreatedEntityOnSuccess() {
    CommandDto dto = new CommandDto("ping", "cmd-1");
    dto.setStatus(StatusDto.ACTIVE);
    CommandEntity saved = new CommandEntity(dto);
    saved.setId("c1");
    when(commandsRepository.insert(any(CommandEntity.class))).thenReturn(saved);

    Optional<CommandEntity> result = commandsService.createCommand(dto);

    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    verify(commandsRepository).insert(any(CommandEntity.class));
  }

  @Test
  void createCommandShouldReturnEmptyOnDuplicateKeyException() {
    CommandDto dto = new CommandDto("ping", "cmd-1");
    when(commandsRepository.insert(any(CommandEntity.class))).thenThrow(new DuplicateKeyException("duplicate"));

    Optional<CommandEntity> result = commandsService.createCommand(dto);

    assertFalse(result.isPresent());
    verify(commandsRepository).insert(any(CommandEntity.class));
  }

  @Test
  void getCommandsShouldReturnAllWhenNameNull() {
    List<CommandEntity> entities = List.of(new CommandEntity());
    when(commandsRepository.findAll()).thenReturn(entities);

    List<CommandEntity> result = commandsService.getCommands(null);

    assertEquals(1, result.size());
    verify(commandsRepository).findAll();
  }

  @Test
  void getCommandsShouldReturnByNameWhenProvided() {
    List<CommandEntity> entities = List.of(new CommandEntity());
    when(commandsRepository.findByName("ping")).thenReturn(entities);

    List<CommandEntity> result = commandsService.getCommands("ping");

    assertEquals(1, result.size());
    verify(commandsRepository).findByName("ping");
  }

  @Test
  void getCommandByIdShouldReturnEmptyWhenIdNull() {
    Optional<CommandEntity> result = commandsService.getCommandById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getCommandByIdShouldReturnEntityWhenFound() {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("ping");
    when(commandsRepository.findById("c1")).thenReturn(Optional.of(entity));

    Optional<CommandEntity> result = commandsService.getCommandById("c1");

    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    verify(commandsRepository).findById("c1");
  }

  @Test
  void getCommandByNameShouldReturnEmptyWhenNameNull() {
    Optional<CommandEntity> result = commandsService.getCommandByName(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getCommandByNameShouldReturnEntityWhenFound() {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("ping");
    when(commandsRepository.findByName("ping")).thenReturn(List.of(entity));

    Optional<CommandEntity> result = commandsService.getCommandByName("ping");

    assertTrue(result.isPresent());
    assertEquals("ping", result.get().getName());
    verify(commandsRepository).findByName("ping");
  }

  @Test
  void getCommandByNameShouldReturnEmptyWhenMultipleFound() {
    CommandEntity first = new CommandEntity();
    first.setId("c1");
    first.setName("ping");
    CommandEntity second = new CommandEntity();
    second.setId("c2");
    second.setName("ping");
    when(commandsRepository.findByName("ping")).thenReturn(List.of(first, second));

    Optional<CommandEntity> result = commandsService.getCommandByName("ping");

    assertFalse(result.isPresent());
    verify(commandsRepository).findByName("ping");
  }

  @Test
  void createCommandShouldPropagateExceptionWhenNonDuplicateKeyException() {
    CommandDto dto = new CommandDto("ping", "cmd-1");
    when(commandsRepository.insert(any(CommandEntity.class)))
      .thenThrow(new RuntimeException("DB connection failed"));

    assertThrows(RuntimeException.class, () -> commandsService.createCommand(dto));
    verify(commandsRepository).insert(any(CommandEntity.class));
  }

  @Test
  void updateCommandShouldReturnEmptyWhenIdNull() {
    CommandDto dto = new CommandDto("ping", "cmd-1");

    Optional<CommandEntity> result = commandsService.updateCommand(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateCommandShouldReturnUpdatedEntityWhenFound() {
    CommandEntity existing = new CommandEntity();
    existing.setId("c1");
    existing.setName("ping");
    existing.setCommandId("cmd-1");
    CommandDto dto = new CommandDto("pong", "cmd-2");
    dto.setStatus(StatusDto.ACTIVE);
    when(commandsRepository.findById("c1")).thenReturn(Optional.of(existing));
    when(commandsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Optional<CommandEntity> result = commandsService.updateCommand("c1", dto);

    assertTrue(result.isPresent());
    assertEquals("pong", result.get().getName());
    verify(commandsRepository).findById("c1");
    verify(commandsRepository).save(any());
  }

  @Test
  void updateCommandShouldReturnEmptyWhenNotFound() {
    CommandDto dto = new CommandDto("ping", "cmd-1");
    when(commandsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<CommandEntity> result = commandsService.updateCommand("missing", dto);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteCommandShouldReturnEmptyWhenIdNull() {
    Optional<CommandEntity> result = commandsService.deleteCommand(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteCommandShouldReturnDeletedEntityWhenFound() {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("ping");
    when(commandsRepository.findById("c1")).thenReturn(Optional.of(entity));

    Optional<CommandEntity> result = commandsService.deleteCommand("c1");

    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    verify(commandsRepository).findById("c1");
    verify(commandsRepository).deleteById("c1");
  }

  @Test
  void deleteCommandShouldReturnEmptyWhenNotFound() {
    when(commandsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<CommandEntity> result = commandsService.deleteCommand("missing");

    assertFalse(result.isPresent());
  }

  @Test
  void updateCommandsShouldReturnEmptyWhenNullOrEmpty() {
    List<CommandDto> resultNull = commandsService.updateCommands(null);
    List<CommandDto> resultEmpty = commandsService.updateCommands(List.of());

    assertTrue(resultNull.isEmpty());
    assertTrue(resultEmpty.isEmpty());
  }

  @Test
  void updateCommandsShouldUpdateExistingCommandWhenCommandIdChanged() {
    CommandEntity existing = new CommandEntity();
    existing.setId("c1");
    existing.setName("ping");
    existing.setCommandId("old-cmd");
    existing.setStatus(StatusDto.ACTIVE);
    CommandDto dto = new CommandDto("ping", "new-cmd");
    dto.setStatus(StatusDto.ACTIVE);
    when(commandsRepository.findAll()).thenReturn(List.of(existing));
    when(commandsRepository.findById("c1")).thenReturn(Optional.of(existing));
    when(commandsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    List<CommandDto> result = commandsService.updateCommands(List.of(dto));

    assertEquals(1, result.size());
    assertEquals(StatusDto.ACTIVE, result.get(0).getStatus());
    verify(commandsRepository).save(any());
  }

  @Test
  void setCommandsShouldReturnFalseWhenCommandNameInvalid() {
    List<CommandDto> captured = new ArrayList<>();
    boolean result = commandsService.setCommands("", "user1", captured::addAll);

    assertFalse(result);
    assertTrue(captured.isEmpty());
  }

  @Test
  void setCommandsShouldReturnTrueAndAcceptCommandsWhenValid() {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("ping");
    entity.setCommandId("cmd-1");
    entity.setStatus(StatusDto.ACTIVE);
    when(commandsRepository.findAll()).thenReturn(List.of(entity));

    List<CommandDto> captured = new ArrayList<>();
    boolean result = commandsService.setCommands("ping", "user1", captured::addAll);

    assertTrue(result);
    assertEquals(1, captured.size());
    assertEquals("ping", captured.get(0).getName());
    verify(commandsRepository).findAll();
  }

  @Test
  void setCommandsShouldReturnFalseWhenCommandLockedAndNotOwner() {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("drop");
    entity.setCommandId("cmd-1");
    entity.setStatus(StatusDto.LOCKED);
    when(commandsRepository.findAll()).thenReturn(List.of(entity));

    List<CommandDto> captured = new ArrayList<>();
    boolean result = commandsService.setCommands("drop", "non-owner-user", captured::addAll);

    assertFalse(result);
    assertEquals(1, captured.size());
    verify(commandsRepository).findAll();
  }

  @Test
  void updateCommandsShouldCreateNewCommandWhenNotExists() {
    CommandDto dto = new CommandDto("newcmd", "cmd-1");
    dto.setStatus(StatusDto.ACTIVE);
    CommandEntity created = new CommandEntity(dto);
    created.setId("c1");
    when(commandsRepository.findAll()).thenReturn(List.of());
    when(commandsRepository.insert(any(CommandEntity.class))).thenReturn(created);

    List<CommandDto> result = commandsService.updateCommands(List.of(dto));

    assertEquals(1, result.size());
    assertEquals(StatusDto.LOCKED, result.get(0).getStatus());
    verify(commandsRepository).insert(any(CommandEntity.class));
  }
}
