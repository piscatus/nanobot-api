package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.TriviaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TriviasRepository
  extends MongoRepository<TriviaEntity, String> {
  @SuppressWarnings("null")
  List<TriviaEntity> findAll();

  Optional<TriviaEntity> findByHash(String hash);

  List<TriviaEntity> findByEnabled(boolean enabled);
}
