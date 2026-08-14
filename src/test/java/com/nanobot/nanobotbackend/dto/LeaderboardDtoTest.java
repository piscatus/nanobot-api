package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaderboardDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    LeaderboardDto dto = new LeaderboardDto();
    assertNull(dto.getGuildId());
    assertNull(dto.getUserId());
    assertNull(dto.getItems());
  }

  @Test
  void fromEntityCopiesAllFields() {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb-1");
    entity.setGuildId("guild-1");
    entity.setUserId("user-1");
    entity.setItems(List.of(new ItemDto("FISH", 5)));

    LeaderboardDto dto = new LeaderboardDto(entity);

    assertEquals("lb-1", dto.getId());
    assertEquals("guild-1", dto.getGuildId());
    assertEquals("user-1", dto.getUserId());
    assertNotNull(dto.getItems());
    assertEquals(1, dto.getItems().size());
    assertEquals("FISH", dto.getItems().get(0).getName());
    assertEquals(5, dto.getItems().get(0).getQuantity());
  }

  @Test
  void settersUpdateFields() {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("guild-2");
    dto.setUserId("user-2");
    dto.setItems(List.of(new ItemDto("CRAB", 10)));

    assertEquals("guild-2", dto.getGuildId());
    assertEquals("user-2", dto.getUserId());
    assertEquals(1, dto.getItems().size());
    assertEquals("CRAB", dto.getItems().get(0).getName());
  }
}
