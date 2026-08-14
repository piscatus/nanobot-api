package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.ArrayList;
import java.util.List;

public class UserWalletsDto extends BaseDto {

  private String userId;

  private List<WalletDto> wallets;

  public UserWalletsDto() {
    super();
    this.wallets = new ArrayList<>();
  }

  public UserWalletsDto(UserWalletsEntity entity) {
    super(entity.getId());
    this.userId = entity.getUserId();
    this.wallets = new ArrayList<>();
    if (entity.getWallets() != null) {
      for (WalletDto wallet : entity.getWallets()) {
        this.wallets.add(new WalletDto(wallet));
      }
    }
  }

  public UserWalletsDto(String userId) {
    super();
    this.userId = userId;
    this.wallets = new ArrayList<>();
  }

  public UserWalletsDto(String userId, List<WalletDto> wallets) {
    super();
    this.userId = userId;
    this.wallets = wallets != null ? wallets : new ArrayList<>();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public List<WalletDto> getWallets() {
    return wallets;
  }

  public void setWallets(List<WalletDto> wallets) {
    this.wallets = wallets;
  }
}
