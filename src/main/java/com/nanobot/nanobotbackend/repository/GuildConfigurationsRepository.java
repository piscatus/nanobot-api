package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildConfigurationsRepository
  extends MongoRepository<GuildConfigurationsEntity, String> {
  @SuppressWarnings("null")
  List<GuildConfigurationsEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }")
  List<GuildConfigurationsEntity> findByGuildId(String guildId);
}
