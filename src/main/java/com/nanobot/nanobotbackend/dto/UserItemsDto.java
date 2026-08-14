package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import java.util.ArrayList;
import java.util.List;

public class UserItemsDto extends BaseDto {

  private String userId;

  private List<ItemDto> items;

  public UserItemsDto() {
    super();
    this.items = new ArrayList<>();
  }

  public UserItemsDto(UserItemsEntity entity) {
    super(entity.getId());
    this.userId = entity.getUserId();
    this.items = new ArrayList<>();
    if (entity.getItems() != null) {
      for (ItemDto item : entity.getItems()) {
        this.items.add(new ItemDto(item));
      }
    }
  }

  public UserItemsDto(String userId) {
    super();
    this.userId = userId;
    this.items = new ArrayList<>();
  }

  public UserItemsDto(String userId, List<ItemDto> items) {
    super();
    this.userId = userId;
    this.items = items != null ? items : new ArrayList<>();
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
