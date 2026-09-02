package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.repository.UserDetailsRepository;
import com.nanobot.nanobotbackend.service.chain.DepositAddressResolver;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  private UserDetailsRepository userDetailsRepository;

  private final DepositAddressResolver depositAddressResolver;

  private final FileLogger fileLogger;

  public UserDetailsServiceImpl(
    UserDetailsRepository userDetailsRepository,
    DepositAddressResolver depositAddressResolver
  ) {
    this.fileLogger = new FileLogger("UserDetailsService");
    this.userDetailsRepository = userDetailsRepository;
    this.depositAddressResolver = depositAddressResolver;
  }

  @Override
  public boolean setUserDetails(
    String userId,
    Consumer<UserDetailsDto> setUserDetails,
    boolean removeSensitiveData
  ) {
    if (StringUtil.isValidString(userId)) {
      UserDetailsDto userDetailsDto = new UserDetailsDto(
        getOrCreateUserDetails(userId)
      );
      if (removeSensitiveData) {
        userDetailsDto.removeSensitiveData();
      }
      setUserDetails.accept(userDetailsDto);
      return userDetailsDto.getStatus() == StatusDto.ACTIVE;
    }
    return false;
  }

  @Override
  public List<WalletDto> generateAddresses(
    List<CurrencyDto> currencies,
    UserDetailsDto userDetails
  ) {
    List<WalletDto> addresses = new ArrayList<>();
    if (userDetails.getSeed() == null) {
      userDetails.setSeed(CryptoUtil.getRandomKey());
      UserDetailsEntity userDetailsEntity = new UserDetailsEntity(userDetails);
      saveUserDetails(userDetailsEntity);
    }
    for (CurrencyDto currencyDto : currencies) {
      if (currencyDto.getEnabled()) {
        String ticker = currencyDto.getTicker();
        // Routed through the adapter rather than derived directly: Nano forks
        // derive from the user's seed, while Monero has to issue a subaddress
        // from the hot wallet.
        String address = depositAddressResolver.resolve(userDetails, ticker);
        if (address == null) {
          fileLogger.warn(
            "Skipping " +
            ticker +
            " deposit address for user " +
            userDetails.getUserId() +
            "; none could be resolved."
          );
          continue;
        }
        addresses.add(new WalletDto(ticker, address, currencyDto.getName()));
      }
    }
    return addresses;
  }

  @Override
  public Optional<UserDetailsEntity> createUserDetails(
    UserDetailsDto userDetailsDto
  ) {
    UserDetailsEntity userDetailsEntity = new UserDetailsEntity(userDetailsDto);
    ObjectId id = new ObjectId();
    userDetailsEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating user details with userId: " + userDetailsEntity.getUserId()
    );
    try {
      UserDetailsEntity createdUser = userDetailsRepository.insert(
        userDetailsEntity
      );
      fileLogger.info("User created with ID: " + createdUser.getId());
      return Optional.of(createdUser);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "User Details already exists with specified userId: " +
        userDetailsDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating user: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<UserDetailsEntity> getUsersDetails(String userId) {
    try {
      if (userId == null) {
        // fileLogger.info("Fetching all users.");
        return userDetailsRepository.findAll();
      } else {
        fileLogger.info("Fetching users with userId: " + userId);
        return userDetailsRepository.findByUserId(userId);
      }
    } catch (Exception e) {
      fileLogger.error("Error fetching users: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<UserDetailsEntity> getUserDetailsById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching user with ID: " + id);
        return userDetailsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching user by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<UserDetailsEntity> getUserDetailsByUserId(String userId) {
    if (userId != null) {
      try {
        // fileLogger.info("Fetching user details with userId: " + userId);
        List<UserDetailsEntity> existingUserDetails =
          userDetailsRepository.findByUserId(userId);
        if (existingUserDetails.size() == 1) {
          return Optional.of(existingUserDetails.get(0));
        } else if (existingUserDetails.size() > 1) {
          fileLogger.error(
            "Multiple users found with the same user ID: " + userId
          );
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error fetching user by userId: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public List<UserDetailsEntity> getActiveUsersDetails() {
    try {
      fileLogger.info("Fetching active users.");
      return userDetailsRepository.findActiveUsers();
    } catch (Exception e) {
      fileLogger.error("Error fetching active users: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<UserDetailsEntity> getLockedUsersDetails() {
    try {
      fileLogger.info("Fetching locked users.");
      return userDetailsRepository.findLockedUsers();
    } catch (Exception e) {
      fileLogger.error("Error fetching locked users: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<UserDetailsEntity> getBannedUsersDetails() {
    try {
      fileLogger.info("Fetching banned users.");
      return userDetailsRepository.findBannedUsers();
    } catch (Exception e) {
      fileLogger.error("Error fetching banned users: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<UserDetailsEntity> updateUserDetails(
    String id,
    UserDetailsDto userDetailsDto
  ) {
    if (id != null) {
      try {
        Optional<UserDetailsEntity> userOptional =
          userDetailsRepository.findById(id);
        if (userOptional.isPresent()) {
          UserDetailsEntity user = userOptional.get();
          user.setStatus(userDetailsDto.getStatus());
          user.setSeed(userDetailsDto.getSeed());
          user.setIndex(userDetailsDto.getIndex());
          user.setPrivateKey(userDetailsDto.getPrivateKey());
          user.setUserId(userDetailsDto.getUserId());
          user.setSubordinateUserId(userDetailsDto.getSubordinateUserId());

          UserDetailsEntity updatedUser = userDetailsRepository.save(user);
          fileLogger.info("User updated with ID: " + updatedUser.getId());
          return Optional.of(updatedUser);
        } else {
          fileLogger.warn("User not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating user: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<UserDetailsEntity> deleteUserDetails(String id) {
    if (id != null) {
      try {
        Optional<UserDetailsEntity> userOptional =
          userDetailsRepository.findById(id);
        if (userOptional.isPresent()) {
          UserDetailsEntity user = userOptional.get();
          userDetailsRepository.deleteById(id);
          fileLogger.info("User deleted with ID: " + id);
          return Optional.of(user);
        } else {
          fileLogger.warn("User not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting user: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public UserDetailsEntity getOrCreateUserDetails(String userId) {
    try {
      fileLogger.info(
        "Fetching or creating user details with userId: " + userId
      );
      List<UserDetailsEntity> existingUserDetails =
        userDetailsRepository.findByUserId(userId);
      if (existingUserDetails.size() == 1) {
        UserDetailsEntity userDetailsEntity = existingUserDetails.get(0);
        if (userDetailsEntity.getSeed() == null) {
          userDetailsEntity.setSeed(CryptoUtil.getRandomKey());
          UserDetailsDto updatedUserDetails = new UserDetailsDto(
            userDetailsEntity
          );
          updateUserDetails(updatedUserDetails.getId(), updatedUserDetails);
          return userDetailsEntity;
        } else {
          return userDetailsEntity;
        }
      } else if (existingUserDetails.size() > 1) {
        throw new Error(
          "Multiple users found with the same user ID: " + userId
        );
      } else {
        UserDetailsDto newUserDetailsDto = new UserDetailsDto(
          userId,
          "",
          StatusDto.ACTIVE,
          CryptoUtil.getRandomKey()
        );

        UserDetailsEntity newUserDetailsEntity = new UserDetailsEntity(
          newUserDetailsDto
        );
        ObjectId id = new ObjectId();
        newUserDetailsEntity.setId(id.toHexString());

        UserDetailsEntity createdUser = userDetailsRepository.save(
          newUserDetailsEntity
        );
        fileLogger.info(
          "New user details created with ID: " + createdUser.getId()
        );
        return createdUser;
      }
    } catch (Exception e) {
      fileLogger.error(
        "Error creating or fetching user details: " + e.getMessage()
      );
      throw e;
    }
  }

  public UserDetailsEntity saveUserDetails(UserDetailsEntity details) {
    return userDetailsRepository.save(details);
  }
}
