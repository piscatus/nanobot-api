package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UserItemsEntityTest {

  @Test
  void testNoArgConstructorAndSetters() {
    String id = "entity001";
    String userId = "user123";
    
    UserItemsEntity entity = new UserItemsEntity();

    entity.setId(id);
    entity.setUserId(userId);

    List<ItemDto> items = new ArrayList<>();
    items.add(new ItemDto("KRAKEN", 7));
    entity.setItems(items);

    assertEquals(id, entity.getId());
    assertEquals(userId, entity.getUserId());
    assertEquals(items, entity.getItems());
  }

  @Test
  void testConstructorWithDto() {
    String userId = "user456";
    List<ItemDto> items = new ArrayList<>();
    items.add(new ItemDto("KRAKEN", 2));

    UserItemsDto dto = new UserItemsDto(userId);
    dto.setItems(items);

    UserItemsEntity entity = new UserItemsEntity(dto);

    assertEquals(userId, entity.getUserId());
    assertEquals(items, entity.getItems());
  }

  @Test
  void testToJson() {
    UserItemsEntity entity = new UserItemsEntity();
    entity.setId("entity002");

    String userId = "user789";
    entity.setUserId(userId);

    List<ItemDto> items = new ArrayList<>();
    String creature = "SHRIMP";
    int quantity = 4;
    items.add(new ItemDto(creature, quantity));
    entity.setItems(items);

    String json = entity.toJson(false);

    assertNotNull(json);
    // Simple sanity check; you can use a JSON parser to be more thorough
    assertEquals(true, json.contains("\"userId\":\"" + userId + "\""));
    assertEquals(true, json.contains("\"name\":\"" + creature + "\""));
    assertEquals(true, json.contains("\"quantity\":" + quantity));
  }

  @Test
  void testToJsonIncludesNullFields() {
      UserItemsEntity entity = new UserItemsEntity();
      entity.setId("entity004");

      String userId = "user123";
      entity.setUserId(userId);

      List<ItemDto> items = new ArrayList<>();
      items.add(new ItemDto(null, 5));
      entity.setItems(items);

      String json = entity.toJson(true);

      assertNotNull(json);
      assertTrue(json.contains("\"userId\":\"" + userId + "\""));
      assertTrue(json.contains("\"quantity\":5"));
      assertTrue(json.contains("\"name\":null"));
  }
}
