package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.ActivityEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivitiesRepository
  extends MongoRepository<ActivityEntity, String> {
  @SuppressWarnings("null")
  List<ActivityEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }")
  List<ActivityEntity> findByGuildId(String guildId);

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'channelId' : [1] } }" +
    "]}"
  )
  List<ActivityEntity> findByGuildIdAndChannelId(
    String guildId,
    String channelId
  );

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'channelId' : [1] } }" +
    "?#{ [2] == null ? { $where : 'true'} : { 'userId' : [2] } }" +
    "]}"
  )
  List<ActivityEntity> findByGuildIdAndChannelIdAndUserId(
    String guildId,
    String channelId,
    String userId
  );
}
