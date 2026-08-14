package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.repository.UserWalletsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserWalletsServiceImpl implements UserWalletsService {

  private UserWalletsRepository userWalletsRepository;

  private final FileLogger fileLogger;

  public UserWalletsServiceImpl(UserWalletsRepository userWalletsRepository) {
    this.fileLogger = new FileLogger("UserWalletsService");
    this.userWalletsRepository = userWalletsRepository;
  }

  @Override
  public void setUserWallets(
    String userId,
    Consumer<List<WalletDto>> setUserWallets
  ) {
    if (StringUtil.isValidString(userId)) {
      List<UserWalletsEntity> existingUserWallets = getUsersWallets(userId);
      if (existingUserWallets.size() == 1) {
        setUserWallets.accept(existingUserWallets.get(0).getWallets());
      }
    }
  }

  @Override
  public void setAllUsersWallets(
    Consumer<List<UserWalletsDto>> setAllUsersWallets
  ) {
    List<UserWalletsEntity> allUserWallets = getUsersWallets(null);
    List<UserWalletsDto> usersWalletsDtos = new ArrayList<>();
    for (UserWalletsEntity userWallets : allUserWallets) {
      usersWalletsDtos.add(new UserWalletsDto(userWallets));
    }
    setAllUsersWallets.accept(usersWalletsDtos);
  }

  @Override
  @Transactional
  public Optional<UserWalletsEntity> createUserWallets(
    UserWalletsDto userWalletsDto
  ) {
    UserWalletsEntity userWalletsEntity = new UserWalletsEntity(userWalletsDto);
    ObjectId id = new ObjectId();
    userWalletsEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating user wallet with userId: " + userWalletsEntity.getUserId()
    );
    // Check if the userId already exists in the repository
    try {
      UserWalletsEntity createdUserWallets = userWalletsRepository.insert(
        userWalletsEntity
      );
      fileLogger.info(
        "User Wallets created with ID: " + createdUserWallets.getId()
      );
      return Optional.of(createdUserWallets);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "User Wallets already exists with specified userId: " +
        userWalletsDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating user wallets: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<UserWalletsEntity> getUsersWallets(String userId) {
    try {
      if (userId == null) {
        // fileLogger.info("Fetching all user wallets.");
        return userWalletsRepository.findAll();
      }
      fileLogger.info("Fetching user wallets with userId: " + userId);
      return userWalletsRepository.findByUserId(userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching user wallets: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<UserWalletsEntity> getUserWalletsById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching user wallets with ID: " + id);
        return userWalletsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching user wallets by ID: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<UserWalletsEntity> getUserWalletsByUserId(String userId) {
    if (userId != null) {
      try {
        fileLogger.info("Fetching user wallets with userId: " + userId);
        List<UserWalletsEntity> existingUserWallets =
          userWalletsRepository.findByUserId(userId);
        if (existingUserWallets.size() > 1) {
          fileLogger.error(
            "Multiple user wallets found with the same userId: " + userId
          );
        } else if (!existingUserWallets.isEmpty()) {
          return Optional.of(existingUserWallets.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching user wallets by userId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public Optional<UserWalletsEntity> updateUserWallets(
    String id,
    UserWalletsDto userWalletsDto
  ) {
    if (id != null) {
      try {
        Optional<UserWalletsEntity> walletsOptional =
          userWalletsRepository.findById(id);
        if (walletsOptional.isPresent()) {
          UserWalletsEntity wallets = walletsOptional.get();
          wallets.setUserId(userWalletsDto.getUserId());
          wallets.setWallets(userWalletsDto.getWallets());

          UserWalletsEntity updatedUserWallets = userWalletsRepository.save(
            wallets
          );
          fileLogger.info("User Wallets updated with ID: " + id);
          return Optional.of(updatedUserWallets);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating user wallets: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public Optional<UserWalletsEntity> deleteUserWallets(String id) {
    if (id != null) {
      try {
        Optional<UserWalletsEntity> walletsOptional =
          userWalletsRepository.findById(id);
        if (walletsOptional.isPresent()) {
          UserWalletsEntity wallets = walletsOptional.get();
          List<WalletDto> emptyWallet = wallets.getWallets();
          for (WalletDto wallet : emptyWallet) {
            wallet.setRaw("0");
          }
          wallets.setWallets(emptyWallet);
          UserWalletsEntity updatedWallets = userWalletsRepository.save(
            wallets
          );
          fileLogger.info("User Wallets data removed with ID: " + id);
          return Optional.of(updatedWallets);
        } else {
          fileLogger.warn("User Wallets not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting user wallets: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Transactional
  public void saveWallet(UserWalletsEntity wallet) {
    try {
      String userId = wallet.getUserId();
      if (!userId.equals("0") && !userId.equals(System.getenv("BOT_USER_ID"))) {
        userWalletsRepository.save(wallet);
      }
    } catch (Exception e) {
      fileLogger.error("Error saving wallet: " + e.getMessage());
      throw e;
    }
  }
}
