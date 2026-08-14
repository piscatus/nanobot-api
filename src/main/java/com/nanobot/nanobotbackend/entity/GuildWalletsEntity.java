package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "guildWallets")
public class GuildWalletsEntity extends BaseEntity {

  @Version
  private Long version;

  @Indexed(unique = true)
  private String guildId;

  private List<WalletDto> wallets;

  public GuildWalletsEntity() {
    super();
  }

  public GuildWalletsEntity(String guildId) {
    super();
    this.guildId = guildId;
    this.wallets = new ArrayList<>();
  }

  public GuildWalletsEntity(GuildWalletsDto guildWallet) {
    super(guildWallet.getId());
    this.guildId = guildWallet.getGuildId();
    this.wallets = guildWallet.getWallets();
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
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
