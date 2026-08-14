package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.AnglerEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AnglersRepository
  extends MongoRepository<AnglerEntity, String> {
  @SuppressWarnings("null")
  List<AnglerEntity> findAll();

  @Query("{ 'resting' : true }")
  List<AnglerEntity> findAllRestingAnglers();

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'userId' : [1] } }" +
    "]}"
  )
  List<AnglerEntity> findByGuildIdAndUserId(String guildId, String userId);
}
