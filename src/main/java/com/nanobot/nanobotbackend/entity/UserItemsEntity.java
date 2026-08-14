package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "userItems")
public class UserItemsEntity extends BaseEntity {

  @Version
  private Long version;

  @Indexed(unique = true)
  private String userId;

  private List<ItemDto> items;

  public UserItemsEntity() {
    super();
  }

  public UserItemsEntity(String userId) {
    super();
    this.userId = userId;
    this.items = new ArrayList<>();
  }

  public UserItemsEntity(UserItemsDto userItems) {
    super(userItems.getId());
    this.userId = userItems.getUserId();
    this.items = userItems.getItems();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public List<ItemDto> getItems() {
    return items;
  }

  public void setItems(List<ItemDto> items) {
    this.items = items;
  }
}
