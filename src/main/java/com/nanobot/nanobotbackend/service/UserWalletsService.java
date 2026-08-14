package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface UserWalletsService {
  Optional<UserWalletsEntity> createUserWallets(UserWalletsDto userWalletsDto);

  List<UserWalletsEntity> getUsersWallets(String userId);

  Optional<UserWalletsEntity> getUserWalletsById(String id);

  Optional<UserWalletsEntity> getUserWalletsByUserId(String userId);

  Optional<UserWalletsEntity> updateUserWallets(
    String id,
    UserWalletsDto userWalletsDto
  );

  Optional<UserWalletsEntity> deleteUserWallets(String id);

  void saveWallet(UserWalletsEntity wallet);

  void setUserWallets(String userId, Consumer<List<WalletDto>> setUserWallets);

  void setAllUsersWallets(Consumer<List<UserWalletsDto>> setAllUserWallets);
}
