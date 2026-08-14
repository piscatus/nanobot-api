package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.List;
import java.util.Optional;

public interface QueuesService {
  QueueEntity createQueue(QueueDto queueDto);

  List<QueueEntity> getQueues();

  Optional<QueueEntity> getQueueById(String id);

  Optional<QueueEntity> updateQueue(String id, QueueDto queueDto);

  Optional<QueueEntity> deleteQueue(String id);
}
