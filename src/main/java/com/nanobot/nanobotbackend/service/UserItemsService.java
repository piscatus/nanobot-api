package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface UserItemsService {
  Optional<UserItemsEntity> createUserItems(UserItemsDto userItemsDto);

  List<UserItemsEntity> getUsersItems(String userId);

  Optional<UserItemsEntity> getUserItemsById(String id);

  Optional<UserItemsEntity> getUserItemsByUserId(String userId);

  Optional<UserItemsEntity> updateUserItems(
    String id,
    UserItemsDto userItemsDto
  );

  Optional<UserItemsEntity> deleteUserItems(String id);

  void saveItem(UserItemsEntity item);

  void setUserItems(String userId, Consumer<List<ItemDto>> setUserItems);

  void setAllUsersItems(Consumer<List<UserItemsDto>> setAllUserItems);
}
