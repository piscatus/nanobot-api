package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "leaderboards")
public class LeaderboardEntity extends BaseEntity {

  @Indexed
  private String guildId;

  @Indexed
  private String userId;

  private List<ItemDto> items;

  public LeaderboardEntity() {
    super();
  }

  public LeaderboardEntity(LeaderboardDto leaderboard) {
    super(leaderboard.getId());
    this.guildId = leaderboard.getGuildId();
    this.userId = leaderboard.getUserId();
    this.items = leaderboard.getItems();
  }

  public LeaderboardEntity(
    String guildId,
    String userId,
    CreatureDto creature
  ) {
    super();
    ItemDto item = new ItemDto(creature.getName().toUpperCase(), 1);
    item.setTimestamp(new Date());
    List<ItemDto> itemList = new ArrayList<>();
    itemList.add(item);
    this.items = itemList;
    this.guildId = guildId;
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public List<ItemDto> getItems() {
    return items;
  }

  public void setItems(List<ItemDto> items) {
    this.items = items;
  }
}
