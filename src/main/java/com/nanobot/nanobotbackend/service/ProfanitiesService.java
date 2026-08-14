package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ProfanityDto;
import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import java.util.List;
import java.util.Optional;

public interface ProfanitiesService {
  Optional<ProfanityEntity> createProfanity(ProfanityDto profanityDto);

  List<ProfanityEntity> getProfanities(String singular);

  Optional<ProfanityEntity> getProfanityById(String id);

  Optional<ProfanityEntity> getProfanityBySingular(String singular);

  Optional<ProfanityEntity> updateProfanity(
    String id,
    ProfanityDto profanityDto
  );

  Optional<ProfanityEntity> deleteProfanity(String id);
}
