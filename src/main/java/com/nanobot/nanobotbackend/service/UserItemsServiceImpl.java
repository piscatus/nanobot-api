package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.repository.UserItemsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserItemsServiceImpl implements UserItemsService {

  private UserItemsRepository userItemsRepository;

  private final FileLogger fileLogger;

  public UserItemsServiceImpl(UserItemsRepository userItemsRepository) {
    this.fileLogger = new FileLogger("UserItemsService");
    this.userItemsRepository = userItemsRepository;
  }

  @Override
  public void setUserItems(
    String userId,
    Consumer<List<ItemDto>> setUserItems
  ) {
    if (StringUtil.isValidString(userId)) {
      List<UserItemsEntity> existingUserItems = getUsersItems(userId);
      if (existingUserItems.size() == 1) {
        setUserItems.accept(existingUserItems.get(0).getItems());
      }
    }
  }

  @Override
  public void setAllUsersItems(Consumer<List<UserItemsDto>> setAllUsersItems) {
    List<UserItemsEntity> allUserItems = getUsersItems(null);
    List<UserItemsDto> userItemsDtos = new ArrayList<>();
    for (UserItemsEntity userItems : allUserItems) {
      userItemsDtos.add(new UserItemsDto(userItems));
    }
    setAllUsersItems.accept(userItemsDtos);
  }

  @Override
  @Transactional
  public Optional<UserItemsEntity> createUserItems(UserItemsDto userItemsDto) {
    UserItemsEntity userItemsEntity = new UserItemsEntity(userItemsDto);
    ObjectId id = new ObjectId();
    userItemsEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating user items with userId: " + userItemsEntity.getUserId()
    );
    try {
      UserItemsEntity createdUserItems = userItemsRepository.insert(
        userItemsEntity
      );
      fileLogger.info(
        "User Items created with ID: " + createdUserItems.getId()
      );
      return Optional.of(createdUserItems);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "User Items already exists with specified userId: " +
        userItemsDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating user items: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<UserItemsEntity> getUsersItems(String userId) {
    try {
      if (userId == null) {
        // fileLogger.info("Fetching all user items.");
        return userItemsRepository.findAll();
      }
      fileLogger.info("Fetching user items with userId: " + userId);
      return userItemsRepository.findByUserId(userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching user items: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<UserItemsEntity> getUserItemsById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching user items with ID: " + id);
        return userItemsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching user items by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<UserItemsEntity> getUserItemsByUserId(String userId) {
    if (userId != null) {
      try {
        fileLogger.info("Fetching user items with userId: " + userId);
        List<UserItemsEntity> existingUserItems =
          userItemsRepository.findByUserId(userId);
        if (existingUserItems.size() > 1) {
          fileLogger.error(
            "Multiple user items found with the same userId: " + userId
          );
        } else if (!existingUserItems.isEmpty()) {
          return Optional.of(existingUserItems.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching user items by userId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public Optional<UserItemsEntity> updateUserItems(
    String id,
    UserItemsDto userItemsDto
  ) {
    if (id != null) {
      try {
        Optional<UserItemsEntity> itemsOptional = userItemsRepository.findById(
          id
        );
        if (itemsOptional.isPresent()) {
          UserItemsEntity items = itemsOptional.get();
          items.setUserId(userItemsDto.getUserId());
          items.setItems(userItemsDto.getItems());

          UserItemsEntity updatedUserItems = userItemsRepository.save(items);
          fileLogger.info("User Items updated with ID: " + id);
          return Optional.of(updatedUserItems);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating user items: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public Optional<UserItemsEntity> deleteUserItems(String id) {
    if (id != null) {
      try {
        Optional<UserItemsEntity> itemsOptional = userItemsRepository.findById(
          id
        );
        if (itemsOptional.isPresent()) {
          UserItemsEntity items = itemsOptional.get();
          List<ItemDto> emptyItems = items.getItems();
          for (ItemDto item : emptyItems) {
            item.setQuantity(0);
          }
          items.setItems(emptyItems);
          UserItemsEntity updatedItems = userItemsRepository.save(items);
          fileLogger.info("User Items data removed with ID: " + id);
          return Optional.of(updatedItems);
        } else {
          fileLogger.warn("User Items not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting user items: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Transactional
  public void saveItem(UserItemsEntity item) {
    try {
      String userId = item.getUserId();
      if (!userId.equals("0") && !userId.equals(System.getenv("BOT_USER_ID"))) {
        userItemsRepository.save(item);
      }
    } catch (Exception e) {
      fileLogger.error("Error saving items: " + e.getMessage());
      throw e;
    }
  }
}
