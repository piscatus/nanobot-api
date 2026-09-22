package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.List;
import java.util.Optional;

public interface QueuesService {
  QueueEntity createQueue(QueueDto queueDto);

  List<QueueEntity> getQueues();

  /**
   * Only the entries for one currency, oldest first.
   *
   * <p>The chain adapters each handle a single currency per pass and skipped
   * every foreign row after loading it, so reading the whole collection was
   * wasted work that grew with every currency added.
   */
  List<QueueEntity> getQueuesByTicker(String ticker);

  /** Whether a deposit with this incoming send hash is already queued. */
  boolean isDepositQueued(String ticker, String sourceHash);

  /** Whether an address is already queued to be swept into the hot wallet. */
  boolean isSweepQueued(String ticker, String sourceAddress);

  /** The queued entry that published this block, once it has been processed. */
  Optional<QueueEntity> getProcessedQueueByBlockHash(
    String ticker,
    String blockHash
  );

  Optional<QueueEntity> getQueueById(String id);

  Optional<QueueEntity> updateQueue(String id, QueueDto queueDto);

  /**
   * Writes only the in-flight progress fields, using a targeted update rather
   * than replacing the document.
   *
   * <p>{@link #updateQueue} copies a fixed set of fields, so a value written on
   * a hot path is silently dropped if it is missing from that list. That is
   * exactly how the withdrawal retry counter came to reset on every pass,
   * leaving a failed withdrawal retrying forever instead of refunding. Null
   * arguments are left untouched.
   */
  boolean updateQueueProgress(
    String id,
    Integer attempts,
    Long index,
    String blockHash,
    Boolean processed
  );

  /**
   * Same as {@link #updateQueueProgress(String, Integer, Long, String, Boolean)}
   * with an optional destination index for a combined Monero send.
   */
  boolean updateQueueProgress(
    String id,
    Integer attempts,
    Long index,
    String blockHash,
    Boolean processed,
    Integer destIndex
  );

  Optional<QueueEntity> deleteQueue(String id);
}
