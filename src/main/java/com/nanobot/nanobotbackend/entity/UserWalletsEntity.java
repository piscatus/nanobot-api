package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "userWallets")
public class UserWalletsEntity extends BaseEntity {

  @Version
  private Long version;

  @Indexed(unique = true)
  private String userId;

  private List<WalletDto> wallets;

  public UserWalletsEntity() {
    super();
  }

  public UserWalletsEntity(String userId) {
    super();
    this.userId = userId;
    this.wallets = new ArrayList<>();
  }

  public UserWalletsEntity(UserWalletsDto userWallets) {
    super(userWallets.getId());
    this.userId = userWallets.getUserId();
    this.wallets = userWallets.getWallets();
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
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
