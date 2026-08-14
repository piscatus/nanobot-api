package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface GuildWalletsService {
  Optional<GuildWalletsEntity> createGuildWallets(
    GuildWalletsDto guildWalletsDto
  );

  List<GuildWalletsEntity> getGuildsWallets(String guildId);

  Optional<GuildWalletsEntity> getGuildWalletsById(String id);

  Optional<GuildWalletsEntity> getGuildWalletsByGuildId(String guildId);

  Optional<GuildWalletsEntity> updateGuildWallets(
    String id,
    GuildWalletsDto guildWalletsDto
  );

  Optional<GuildWalletsEntity> deleteGuildWallets(String id);

  void saveWallet(GuildWalletsEntity wallet);

  void setGuildWallets(
    String guildId,
    Consumer<List<WalletDto>> setGuildWallets
  );

  void setAllGuildsWallets(Consumer<List<GuildWalletsDto>> setAllGuildWallets);
}
