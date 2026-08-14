package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CreaturesService {
  Optional<CreatureEntity> createCreature(CreatureDto creatureDto);

  List<CreatureEntity> getCreatures();

  Optional<CreatureEntity> getCreatureById(String id);

  Optional<CreatureEntity> getCreatureByName(String name);

  Optional<CreatureEntity> updateCreature(String id, CreatureDto creatureDto);

  Optional<CreatureEntity> deleteCreature(String id);

  void setCreatures(Consumer<List<CreatureDto>> setCreatures);

  CreatureDto analyzeCreatures(String inputString);
}
