package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.repository.GuildWalletsRepository;
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
public class GuildWalletsServiceImpl implements GuildWalletsService {

  private GuildWalletsRepository guildWalletsRepository;

  private final FileLogger fileLogger;

  public GuildWalletsServiceImpl(
    GuildWalletsRepository guildWalletsRepository
  ) {
    this.fileLogger = new FileLogger("GuildWalletsService");
    this.guildWalletsRepository = guildWalletsRepository;
  }

  @Override
  public void setGuildWallets(
    String guildId,
    Consumer<List<WalletDto>> setGuildWallets
  ) {
    if (StringUtil.isValidString(guildId)) {
      List<GuildWalletsEntity> existingGuildWallets = getGuildsWallets(guildId);
      if (existingGuildWallets.size() == 1) {
        setGuildWallets.accept(existingGuildWallets.get(0).getWallets());
      }
    }
  }

  @Override
  public void setAllGuildsWallets(
    Consumer<List<GuildWalletsDto>> setGuildsWallets
  ) {
    List<GuildWalletsEntity> allGuildWallets = getGuildsWallets(null);
    List<GuildWalletsDto> guildsWalletsDtos = new ArrayList<>();
    for (GuildWalletsEntity guildWallets : allGuildWallets) {
      guildsWalletsDtos.add(new GuildWalletsDto(guildWallets));
    }
    setGuildsWallets.accept(guildsWalletsDtos);
  }

  @Override
  @Transactional
  public Optional<GuildWalletsEntity> createGuildWallets(
    GuildWalletsDto guildWalletsDto
  ) {
    GuildWalletsEntity guildWalletsEntity = new GuildWalletsEntity(
      guildWalletsDto
    );
    ObjectId id = new ObjectId();
    guildWalletsEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating guild wallet with guildId: " + guildWalletsEntity.getGuildId()
    );
    // Check if the guildId already exists in the repository
    try {
      GuildWalletsEntity createdGuildWallets = guildWalletsRepository.insert(
        guildWalletsEntity
      );
      fileLogger.info(
        "Guild Wallets created with ID: " + createdGuildWallets.getId()
      );
      return Optional.of(createdGuildWallets);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Guild Wallets already exists with specified guildId: " +
        guildWalletsDto.getGuildId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating guild wallets: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<GuildWalletsEntity> getGuildsWallets(String guildId) {
    try {
      if (guildId == null) {
        // fileLogger.info("Fetching all guild wallets.");
        return guildWalletsRepository.findAll();
      }
      fileLogger.info("Fetching guild wallets with guildId: " + guildId);
      return guildWalletsRepository.findByGuildId(guildId);
    } catch (Exception e) {
      fileLogger.error("Error fetching guild wallets: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<GuildWalletsEntity> getGuildWalletsById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching guild wallets with ID: " + id);
        return guildWalletsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching guild wallets by ID: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<GuildWalletsEntity> getGuildWalletsByGuildId(String guildId) {
    if (guildId != null) {
      try {
        fileLogger.info("Fetching guild wallets with guildId: " + guildId);
        List<GuildWalletsEntity> existingGuildWallets =
          guildWalletsRepository.findByGuildId(guildId);
        if (existingGuildWallets.size() > 1) {
          fileLogger.error(
            "Multiple guild wallets found with the same guildId: " + guildId
          );
        } else if (!existingGuildWallets.isEmpty()) {
          return Optional.of(existingGuildWallets.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching guild wallets by guildId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public Optional<GuildWalletsEntity> updateGuildWallets(
    String id,
    GuildWalletsDto guildWalletsDto
  ) {
    if (id != null) {
      try {
        Optional<GuildWalletsEntity> walletsOptional =
          guildWalletsRepository.findById(id);
        if (walletsOptional.isPresent()) {
          GuildWalletsEntity wallets = walletsOptional.get();
          wallets.setGuildId(guildWalletsDto.getGuildId());
          wallets.setWallets(guildWalletsDto.getWallets());

          GuildWalletsEntity updatedGuildWallets = guildWalletsRepository.save(
            wallets
          );
          fileLogger.info("Guild Wallets updated with ID: " + id);
          return Optional.of(updatedGuildWallets);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating guild wallets: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public Optional<GuildWalletsEntity> deleteGuildWallets(String id) {
    if (id != null) {
      try {
        Optional<GuildWalletsEntity> walletsOptional =
          guildWalletsRepository.findById(id);
        if (walletsOptional.isPresent()) {
          GuildWalletsEntity wallets = walletsOptional.get();
          List<WalletDto> emptyWallet = wallets.getWallets();
          for (WalletDto wallet : emptyWallet) {
            wallet.setRaw("0");
          }
          wallets.setWallets(emptyWallet);
          GuildWalletsEntity updatedWallets = guildWalletsRepository.save(
            wallets
          );
          fileLogger.info("Guild Wallets data removed with ID: " + id);
          return Optional.of(updatedWallets);
        } else {
          fileLogger.warn("Guild Wallets not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting guild wallets: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Transactional
  public void saveWallet(GuildWalletsEntity wallet) {
    try {
      guildWalletsRepository.save(wallet);
    } catch (Exception e) {
      fileLogger.error("Error saving wallet: " + e.getMessage());
      throw e;
    }
  }
}
