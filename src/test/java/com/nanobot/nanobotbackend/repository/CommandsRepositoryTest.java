package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class CommandsRepositoryTest {

  @Autowired
  private CommandsRepository commandsRepository;

  @BeforeEach
  void setUp() {
    commandsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoCommands() {
    List<CommandEntity> result = commandsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedCommand() {
    CommandEntity command = new CommandEntity();
    command.setName("balance");
    command.setCommandId("cmd-balance");
    command.setStatus(StatusDto.ACTIVE);

    CommandEntity saved = commandsRepository.insert(command);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<CommandEntity> all = commandsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("balance", all.get(0).getName());
  }

  @Test
  void findByNameReturnsMatchingCommand() {
    CommandEntity command = new CommandEntity();
    command.setName("balance");
    command.setCommandId("cmd-balance");
    commandsRepository.insert(command);

    CommandEntity c2 = new CommandEntity();
    c2.setName("drop");
    c2.setCommandId("cmd-drop");
    commandsRepository.insert(c2);

    List<CommandEntity> byBalance = commandsRepository.findByName("balance");
    assertEquals(1, byBalance.size());
    assertEquals("balance", byBalance.get(0).getName());
  }

  @Test
  void findByNameWithNullReturnsAll() {
    CommandEntity command = new CommandEntity();
    command.setName("balance");
    commandsRepository.insert(command);

    List<CommandEntity> result = commandsRepository.findByName(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    CommandEntity command = new CommandEntity();
    command.setName("balance");
    CommandEntity saved = commandsRepository.insert(command);

    var found = commandsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("balance", found.get().getName());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = commandsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    CommandEntity command = new CommandEntity();
    command.setName("balance");
    command.setStatus(StatusDto.ACTIVE);
    CommandEntity saved = commandsRepository.insert(command);

    saved.setName("wallet");
    saved.setStatus(StatusDto.LOCKED);
    CommandEntity updated = commandsRepository.save(saved);

    assertEquals("wallet", updated.getName());
    assertEquals(StatusDto.LOCKED, updated.getStatus());
  }

  @Test
  void deleteByIdRemovesEntity() {
    CommandEntity command = new CommandEntity();
    command.setName("balance");
    CommandEntity saved = commandsRepository.insert(command);

    commandsRepository.deleteById(saved.getId());

    var found = commandsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
