package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfanitiesRepository
  extends MongoRepository<ProfanityEntity, String> {
  @SuppressWarnings("null")
  List<ProfanityEntity> findAll();

  @Query(
    "?#{ [0] == null ? { $where : 'true' } : { 'singular' : { $regex: '(?i)^' + [0] + '$' } } }"
  )
  List<ProfanityEntity> findBySingular(String singular);
}
