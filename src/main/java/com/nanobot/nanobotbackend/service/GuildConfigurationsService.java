package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface GuildConfigurationsService {
  Optional<GuildConfigurationsEntity> createGuildConfiguration(
    GuildConfigurationsDto guildConfigurationsDto
  );

  List<GuildConfigurationsEntity> getGuildConfigurations(String guildId);

  Optional<GuildConfigurationsEntity> getGuildConfigurationById(String id);

  Optional<GuildConfigurationsEntity> getGuildConfigurationByGuildId(
    String guildId
  );

  Optional<GuildConfigurationsEntity> updateGuildConfiguration(
    String id,
    GuildConfigurationsDto guildConfigurationsDto
  );

  Optional<GuildConfigurationsEntity> deleteGuildConfiguration(String id);

  GuildConfigurationsEntity getOrCreateGuildConfiguration(String guildId);

  GuildConfigurationsEntity saveGuildConfigurations(
    GuildConfigurationsEntity guildConfigurationsEntity
  );

  GuildConfigurationsEntity insertGuildConfigurations(
    GuildConfigurationsEntity guildConfigurationsEntity
  );

  boolean setGuildConfigurations(
    String guildId,
    Consumer<GuildConfigurationsDto> setGuildConfigurations
  );
}
