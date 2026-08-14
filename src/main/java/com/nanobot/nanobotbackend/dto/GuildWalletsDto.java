package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import java.util.ArrayList;
import java.util.List;

public class GuildWalletsDto extends BaseDto {

  private String guildId;

  private List<WalletDto> wallets;

  public GuildWalletsDto() {
    super();
    this.wallets = new ArrayList<>();
  }

  public GuildWalletsDto(GuildWalletsEntity guildWalletsEntity) {
    super(guildWalletsEntity.getId());
    this.guildId = guildWalletsEntity.getGuildId();
    this.wallets = new ArrayList<>();

    if (guildWalletsEntity.getWallets() != null) {
      for (WalletDto wallet : guildWalletsEntity.getWallets()) {
        this.wallets.add(new WalletDto(wallet));
      }
    }
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public List<WalletDto> getWallets() {
    return wallets;
  }

  public void setWallets(List<WalletDto> wallets) {
    this.wallets = wallets;
  }
}
