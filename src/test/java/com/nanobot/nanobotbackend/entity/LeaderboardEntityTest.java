package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaderboardEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    LeaderboardEntity entity = new LeaderboardEntity();
    assertNull(entity.getGuildId());
    assertNull(entity.getUserId());
    assertNull(entity.getItems());
  }

  @Test
  void fromLeaderboardDtoCopiesAllFields() {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setId("lb-1");
    dto.setGuildId("guild-1");
    dto.setUserId("user-1");
    dto.setItems(List.of(new ItemDto("FISH", 5)));

    LeaderboardEntity entity = new LeaderboardEntity(dto);

    assertEquals("lb-1", entity.getId());
    assertEquals("guild-1", entity.getGuildId());
    assertEquals("user-1", entity.getUserId());
    assertNotNull(entity.getItems());
    assertEquals(1, entity.getItems().size());
    assertEquals("FISH", entity.getItems().get(0).getName());
    assertEquals(5, entity.getItems().get(0).getQuantity());
  }

  @Test
  void fromCreatureConstructorCreatesEntityWithItem() {
    CreatureDto creature = new CreatureDto();
    creature.setName("salmon");

    LeaderboardEntity entity = new LeaderboardEntity("guild-2", "user-2", creature);

    assertEquals("guild-2", entity.getGuildId());
    assertEquals("user-2", entity.getUserId());
    assertNotNull(entity.getItems());
    assertEquals(1, entity.getItems().size());
    assertEquals("SALMON", entity.getItems().get(0).getName());
    assertEquals(1, entity.getItems().get(0).getQuantity());
    assertNotNull(entity.getItems().get(0).getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setGuildId("guild-3");
    entity.setUserId("user-3");
    entity.setItems(List.of(new ItemDto("CRAB", 10)));

    assertEquals("guild-3", entity.getGuildId());
    assertEquals("user-3", entity.getUserId());
    assertEquals(1, entity.getItems().size());
    assertEquals("CRAB", entity.getItems().get(0).getName());
  }
}
