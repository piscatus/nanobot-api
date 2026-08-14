package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.CreatureEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CreaturesRepository
  extends MongoRepository<CreatureEntity, String> {
  @SuppressWarnings("null")
  List<CreatureEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'name' : [0] } }")
  List<CreatureEntity> findByName(String name);
}
