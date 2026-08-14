package com.nanobot.nanobotbackend.repository;

import com.nanobot.nanobotbackend.entity.PickupEntity;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PickupsRepository
  extends MongoRepository<PickupEntity, String> {
  @SuppressWarnings("null")
  List<PickupEntity> findAll();

  @Query("?#{ [0] == null ? { $where : 'true'} : { 'dropId' : [0] } }")
  List<PickupEntity> findByDropId(String dropId);

  @Query(
    "{ $and :[" +
    "?#{ [0] == null ? { $where : 'true'} : { 'dropId' : [0] } }" +
    "?#{ [1] == null ? { $where : 'true'} : { 'userId' : [1] } }" +
    "]}"
  )
  List<PickupEntity> findByDropIdAndUserId(String dropId, String userId);
}
