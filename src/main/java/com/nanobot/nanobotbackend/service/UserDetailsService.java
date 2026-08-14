package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface UserDetailsService {
  Optional<UserDetailsEntity> createUserDetails(UserDetailsDto userDetailsDto);

  List<UserDetailsEntity> getUsersDetails(String userId);

  Optional<UserDetailsEntity> getUserDetailsById(String id);

  Optional<UserDetailsEntity> getUserDetailsByUserId(String userId);

  List<UserDetailsEntity> getActiveUsersDetails();

  List<UserDetailsEntity> getLockedUsersDetails();

  List<UserDetailsEntity> getBannedUsersDetails();

  Optional<UserDetailsEntity> updateUserDetails(
    String id,
    UserDetailsDto userDetailsDto
  );

  Optional<UserDetailsEntity> deleteUserDetails(String id);

  UserDetailsEntity getOrCreateUserDetails(String userId);

  UserDetailsEntity saveUserDetails(UserDetailsEntity details);

  List<WalletDto> generateAddresses(
    List<CurrencyDto> currencies,
    UserDetailsDto userDetails
  );

  boolean setUserDetails(
    String userId,
    Consumer<UserDetailsDto> setUserDetails,
    boolean removeSensitiveData
  );
}
