package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.repository.QueuesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class QueuesServiceImpl implements QueuesService {

  private QueuesRepository queuesRepository;

  private final FileLogger fileLogger;

  public QueuesServiceImpl(QueuesRepository queuesRepository) {
    this.fileLogger = new FileLogger("QueuesService");
    this.queuesRepository = queuesRepository;
  }

  @Override
  public QueueEntity createQueue(QueueDto queueDto) {
    QueueEntity queueEntity = new QueueEntity(queueDto);
    ObjectId id = new ObjectId();
    queueEntity.setId(id.toHexString());
    fileLogger.info("Creating queue with ID: " + queueEntity.getId());
    try {
      QueueEntity createdQueue = queuesRepository.insert(queueEntity);
      fileLogger.info("Queue created with ID: " + createdQueue);
      return createdQueue;
    } catch (Exception e) {
      fileLogger.error("Error creating queue: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<QueueEntity> getQueues() {
    try {
      // fileLogger.info("Fetching all queues by oldest timestamp.");
      return queuesRepository.findAllSortedByOldestTimestamp();
    } catch (Exception e) {
      fileLogger.error("Error fetching queues: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<QueueEntity> getQueueById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching queue with ID: " + id);
        return queuesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching queue: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<QueueEntity> updateQueue(String id, QueueDto queueDto) {
    if (id != null) {
      try {
        Optional<QueueEntity> queuesOptional = queuesRepository.findById(id);
        if (queuesOptional.isPresent()) {
          QueueEntity queues = queuesOptional.get();
          queues.setUserId(queueDto.getUserId());
          queues.setSourceAddress(queueDto.getSourceAddress());
          queues.setTargetAddress(queueDto.getTargetAddress());
          queues.setLevel(queueDto.getLevel());
          queues.setBlockHash(queueDto.getBlockHash());
          queues.setRaw(queueDto.getRaw());
          queues.setTicker(queueDto.getTicker());
          queues.setProcessed(queueDto.getProcessed());
          queues.setTimestamp(queueDto.getTimestamp());

          QueueEntity updatedQueue = queuesRepository.save(queues);
          fileLogger.info("Queue updated with ID: " + id);
          return Optional.of(updatedQueue);
        } else {
          fileLogger.warn("Queue not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating queue: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<QueueEntity> deleteQueue(String id) {
    if (id != null) {
      try {
        Optional<QueueEntity> queuesOptional = queuesRepository.findById(id);
        if (queuesOptional.isPresent()) {
          QueueEntity queues = queuesOptional.get();
          queuesRepository.deleteById(id);
          fileLogger.info("Queue deleted with ID: " + id);
          return Optional.of(queues);
        } else {
          fileLogger.warn("Queue not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting queue: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }
}
