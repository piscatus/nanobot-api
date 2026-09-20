package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import java.util.List;
import java.util.Optional;

public interface AnglersService {
  Optional<AnglerEntity> createAngler(AnglerDto anglerDto);

  List<AnglerEntity> getAnglers(String guildId, String userId);

  List<AnglerEntity> getRestingAnglers();

  Optional<AnglerEntity> getAnglerById(String id);

  Optional<AnglerEntity> getAnglerByGuildIdAndUserId(
    String guildId,
    String userId
  );

  Optional<AnglerEntity> updateAngler(String id, AnglerDto anglerDto);

  Optional<AnglerEntity> updateAnglerRestless(String id);

  Optional<AnglerEntity> deleteAngler(String id);

  AnglerEntity updateOrCreateAngler(String guildId, String userId);

  AnglerEntity setAnglerTicker(String guildId, String userId, String ticker);

  AnglerEntity stampAnglerCatch(AnglerEntity angler);
}
