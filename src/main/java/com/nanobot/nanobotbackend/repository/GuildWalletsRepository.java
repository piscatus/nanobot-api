package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildWalletsRepository
  extends MongoRepository<GuildWalletsEntity, String> {
  @SuppressWarnings("null")
  List<GuildWalletsEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'guildId' : [0] } }")
  List<GuildWalletsEntity> findByGuildId(String guildId);
}
