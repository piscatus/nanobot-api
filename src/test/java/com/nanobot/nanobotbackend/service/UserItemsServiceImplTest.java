package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.repository.UserItemsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserItemsServiceImplTest {

  @InjectMocks
  private UserItemsServiceImpl userItemsService;

  @Mock
  private UserItemsRepository userItemsRepositoryMock;

  @Mock
  private FileLogger fileLoggerMock;

  public UserItemsServiceImplTest() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testCreateUserItems() {
    String generatedId = "generated-id-123";
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsDto dto = new UserItemsDto(userId, items);

    UserItemsEntity savedEntity = new UserItemsEntity(dto);
    savedEntity.setId(generatedId);

    when(userItemsRepositoryMock.insert(any(UserItemsEntity.class)))
        .thenReturn(savedEntity);

    Optional<UserItemsEntity> result = userItemsService.createUserItems(dto);

    assertTrue(result.isPresent());
    UserItemsEntity resultEntity = result.get();
    assertEquals(generatedId, resultEntity.getId());
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(items, resultEntity.getItems());
    verify(userItemsRepositoryMock).insert(any(UserItemsEntity.class));
  }

  @Test
  void testGetUsersItems() {
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsEntity savedEntity = new UserItemsEntity(new UserItemsDto(userId, items));
    savedEntity.setId("generated-id-123");

    when(userItemsRepositoryMock.findByUserId(userId))
        .thenReturn(List.of(savedEntity));

    List<UserItemsEntity> result = userItemsService.getUsersItems(userId);

    assertFalse(result.isEmpty());
    UserItemsEntity resultEntity = result.get(0);
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(items, resultEntity.getItems());
  }

  @Test
  void testGetUserItemsById() {
    String id = "generated-id-123";
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsEntity savedEntity = new UserItemsEntity(new UserItemsDto(userId, items));
    savedEntity.setId(id);

    when(userItemsRepositoryMock.findById(id))
        .thenReturn(Optional.of(savedEntity));

    Optional<UserItemsEntity> result = userItemsService.getUserItemsById(id);

    assertTrue(result.isPresent());
    UserItemsEntity resultEntity = result.get();
    assertEquals(id, resultEntity.getId());
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(items, resultEntity.getItems());
  }

  @Test
  void testGetUserItemsByUserId() {
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsEntity savedEntity = new UserItemsEntity(new UserItemsDto(userId, items));
    savedEntity.setId("generated-id-123");

    when(userItemsRepositoryMock.findByUserId(userId))
        .thenReturn(List.of(savedEntity));

    Optional<UserItemsEntity> result = userItemsService.getUserItemsByUserId(userId);

    assertTrue(result.isPresent());
    UserItemsEntity resultEntity = result.get();
    assertEquals(userId, resultEntity.getUserId());
    assertEquals(items, resultEntity.getItems());
  }

  @Test
  void testUpdateUserItems() {
    String id = "update123";
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsEntity existingEntity = new UserItemsEntity(new UserItemsDto(userId, items));
    existingEntity.setId(id);

    ItemDto updatedItem1 = new ItemDto("Item1", 150);
    ItemDto updatedItem2 = new ItemDto("Item2", 250);
    List<ItemDto> updatedItems = List.of(updatedItem1, updatedItem2);
    UserItemsDto updatedDto = new UserItemsDto(userId, updatedItems);

    when(userItemsRepositoryMock.findById(id))
        .thenReturn(Optional.of(existingEntity));
    when(userItemsRepositoryMock.save(any(UserItemsEntity.class)))
        .thenReturn(existingEntity);

    Optional<UserItemsEntity> result = userItemsService.updateUserItems(id, updatedDto);

    assertTrue(result.isPresent());
    UserItemsEntity updatedEntity = result.get();
    assertEquals(userId, updatedEntity.getUserId());
    assertEquals(updatedItems, updatedEntity.getItems());
  }

  @Test
  void testDeleteUserItems() {
    String id = "delete123";
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsEntity existingEntity = new UserItemsEntity(new UserItemsDto(userId, items));
    existingEntity.setId(id);

    when(userItemsRepositoryMock.findById(id))
        .thenReturn(Optional.of(existingEntity));
    when(userItemsRepositoryMock.save(any(UserItemsEntity.class)))
        .thenReturn(existingEntity);

    Optional<UserItemsEntity> result = userItemsService.deleteUserItems(id);

    assertTrue(result.isPresent());
    UserItemsEntity deletedEntity = result.get();
    assertEquals(0, deletedEntity.getItems().get(0).getQuantity());
    assertEquals(0, deletedEntity.getItems().get(1).getQuantity());
  }

  @Test
  void saveItemShouldPersistForNormalUser() {
    UserItemsEntity entity = new UserItemsEntity("user123");
    entity.setId("id1");
    when(userItemsRepositoryMock.save(any(UserItemsEntity.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    userItemsService.saveItem(entity);

    verify(userItemsRepositoryMock).save(entity);
  }

  @Test
  void saveItemShouldNotPersistForSystemUserZero() {
    UserItemsEntity entity = new UserItemsEntity("0");
    entity.setId("id0");

    userItemsService.saveItem(entity);

    verify(userItemsRepositoryMock, never()).save(any(UserItemsEntity.class));
  }

  @Test
  void saveItemShouldNotPersistForBotUserId() {
    String botUserId = System.getenv("BOT_USER_ID");
    if (botUserId == null) {
      botUserId = "test-bot-user-id";
    }
    UserItemsEntity entity = new UserItemsEntity(botUserId);
    entity.setId("id-bot");

    userItemsService.saveItem(entity);

    verify(userItemsRepositoryMock, never()).save(any(UserItemsEntity.class));
  }

  @Test
  void testCreateUserItemsReturnsEmptyWhenDuplicateKey() {
    UserItemsDto dto = new UserItemsDto("user1", List.of(new ItemDto("SHRIMP", 5)));
    when(userItemsRepositoryMock.insert(any(UserItemsEntity.class)))
      .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate userId"));

    Optional<UserItemsEntity> result = userItemsService.createUserItems(dto);

    assertTrue(result.isEmpty());
    verify(userItemsRepositoryMock).insert(any(UserItemsEntity.class));
  }

  @Test
  void testSetUserItems() {
    String userId = "abc123";
    ItemDto item1 = new ItemDto("Item1", 100);
    ItemDto item2 = new ItemDto("Item2", 200);
    List<ItemDto> items = List.of(item1, item2);
    UserItemsEntity existingEntity = new UserItemsEntity(new UserItemsDto(userId, items));
    existingEntity.setId("generated-id-123");

    when(userItemsRepositoryMock.findByUserId(userId))
        .thenReturn(List.of(existingEntity));

    userItemsService.setUserItems(userId, itemList -> {
      assertEquals(items, itemList);
    });

    verify(userItemsRepositoryMock).findByUserId(userId);
  }
}
