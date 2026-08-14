package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.AliasEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AliasesRepository
  extends MongoRepository<AliasEntity, String> {
  @SuppressWarnings("null")
  List<AliasEntity> findAll();

  @Query(
    "{ $and : [" +
    "?#{ [0] == null ? { $where : 'true' } : { 'guildId' : [0] } }, " +
    "?#{ [1] == null ? { $where : 'true' } : { 'singular' : { $regex: '(?i)^' + [1] + '$' } } } " +
    "]}"
  )
  List<AliasEntity> findByGuildIdAndSingular(String guildId, String singular);

  @Query(
    "?#{ [0] == null ? { $where : 'true' } : { 'singular' : { $regex: '(?i)^' + [0] + '$' } } }"
  )
  List<AliasEntity> findBySingular(String singular);

  @Query("?#{ [0] == null ? { $where : 'true' } : { 'guildId' : [0] } }")
  List<AliasEntity> findByGuildId(String guildId);

  @Query("{ $or : [ { 'guildId' : ?0 }, { 'guildId' : ?1 } ] }")
  List<AliasEntity> findByEitherGuildId(String guildId1, String guildId2);
}
