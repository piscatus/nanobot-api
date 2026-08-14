package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.EmojiEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmojisRepository
  extends MongoRepository<EmojiEntity, String> {
  @SuppressWarnings("null")
  List<EmojiEntity> findAll();

  List<EmojiEntity> findByCategory(String category);

  List<EmojiEntity> findByName(String name);

  List<EmojiEntity> findByCategoryAndName(String category, String name);
}
