package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.CommandEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandsRepository
  extends MongoRepository<CommandEntity, String> {
  @SuppressWarnings("null")
  List<CommandEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'name' : [0] } }")
  List<CommandEntity> findByName(String name);
}
