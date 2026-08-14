package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.entity.AliasEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface AliasesService {
  Optional<AliasEntity> createAlias(AliasDto aliasDto);

  List<AliasEntity> getAliases(String guildId, String singular);

  List<AliasEntity> getAliasesByEitherGuildId(String guildId1, String guildId2);

  Optional<AliasEntity> getAliasById(String id);

  Optional<AliasEntity> getAliasByGuildIdAndSingular(
    String guildId,
    String singular
  );

  Optional<AliasEntity> updateAlias(String id, AliasDto aliasDto);

  Optional<AliasEntity> deleteAlias(String id);

  AliasDto analyzeAliases(String guildId, String inputString);

  void setAliases(Consumer<List<AliasDto>> setAliases, String guildId);
}
