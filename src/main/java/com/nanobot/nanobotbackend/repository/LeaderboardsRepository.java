package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderboardsRepository
  extends MongoRepository<LeaderboardEntity, String> {
  @SuppressWarnings("null")
  List<LeaderboardEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }")
  List<LeaderboardEntity> findByGuildId(String guildId);

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'userId' : [1] } }" +
    "]}"
  )
  List<LeaderboardEntity> findByGuildIdAndUserId(String guildId, String userId);
}
