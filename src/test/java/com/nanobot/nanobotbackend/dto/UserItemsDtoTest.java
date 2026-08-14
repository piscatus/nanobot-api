package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UserItemsDtoTest {

  @Test
  void testNoArgConstructorAndSetters() {
    UserItemsDto dto = new UserItemsDto();
    dto.setId("id001");
    dto.setUserId("user123");

    List<ItemDto> items = new ArrayList<>();
    items.add(new ItemDto("SHARK", 15));
    dto.setItems(items);

    assertEquals("id001", dto.getId());
    assertEquals("user123", dto.getUserId());
    assertEquals(items, dto.getItems());
  }

  @Test
  void testConstructorWithUserId() {
    UserItemsDto dto = new UserItemsDto("user456");

    assertEquals("user456", dto.getUserId());
    assertEquals(0, dto.getItems().size());
  }

  @Test
  void testAddItemsToList() {
    UserItemsDto dto = new UserItemsDto("user789");

    List<ItemDto> items = new ArrayList<>();
    items.add(new ItemDto("WHALE", 5));
    items.add(new ItemDto("DOLPHIN", 3));
    dto.setItems(items);

    assertEquals(2, dto.getItems().size());
    assertEquals("WHALE", dto.getItems().get(0).getName());
    assertEquals(5, dto.getItems().get(0).getQuantity());
  }
}
