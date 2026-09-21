package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.DepositNoticeEntity;
import com.nanobot.nanobotbackend.repository.DepositNoticesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Service;

/**
 * Remembers which incoming deposits have already been announced as discovered,
 * so the announcement goes out exactly once per deposit however many times the
 * scan sees the transaction before it confirms.
 */
@Service
public class DepositNoticeService {

  private final DepositNoticesRepository depositNoticesRepository;
  private final MongoTemplate mongoTemplate;
  private final FileLogger fileLogger;

  public DepositNoticeService(
    DepositNoticesRepository depositNoticesRepository,
    MongoTemplate mongoTemplate
  ) {
    this.depositNoticesRepository = depositNoticesRepository;
    this.mongoTemplate = mongoTemplate;
    this.fileLogger = new FileLogger("DepositNoticeService");
  }

  /**
   * Creates the collection's indexes if they are missing.
   *
   * <p>The application builds its MongoTemplate by hand, and Spring Data does
   * not create indexes from entity annotations unless told to, so the unique
   * key that makes {@link #recordFirstSighting} safe would otherwise never
   * exist. Doing it here means neither dev nor prod needs a manual step.
   * ensureIndex is a no-op when the index already exists.
   */
  @PostConstruct
  void ensureIndexes() {
    try {
      IndexOperations indexOperations = mongoTemplate.indexOps(
        DepositNoticeEntity.class
      );
      indexOperations.ensureIndex(
        new Index()
          .named("ticker_txid_index_unique")
          .on("ticker", Sort.Direction.ASC)
          .on("txid", Sort.Direction.ASC)
          .on("addressIndex", Sort.Direction.ASC)
          .unique()
      );
      Duration ttl = Duration.ofSeconds(DepositNoticeEntity.TTL_SECONDS);
      boolean recreateTtl = indexOperations
        .getIndexInfo()
        .stream()
        .filter(info -> "timestamp_ttl".equals(info.getName()))
        .findFirst()
        .map(info -> !ttl.equals(info.getExpireAfter().orElse(Duration.ZERO)))
        .orElse(false);
      if (recreateTtl) {
        indexOperations.dropIndex("timestamp_ttl");
      }
      indexOperations.ensureIndex(
        new Index()
          .named("timestamp_ttl")
          .on("timestamp", Sort.Direction.ASC)
          .expire(ttl)
      );
    } catch (RuntimeException e) {
      // Startup must not depend on Mongo being reachable at this exact moment;
      // without the unique index the worst case is a repeated notice, which is
      // worth a loud log line rather than a refusal to start.
      fileLogger.error(
        "Could not ensure depositNotices indexes: " + e.getMessage()
      );
    }
  }

  /**
   * Records that a deposit has been seen. Returns true only for the first
   * sighting, which is the caller's cue to notify the user; every later call
   * for the same deposit returns false.
   *
   * <p>The unique index does the deduplication, so two scans racing on the same
   * transaction still produce a single true.
   */
  public boolean recordFirstSighting(
    String ticker,
    String txid,
    Long addressIndex,
    String userId,
    String raw
  ) {
    if (
      depositNoticesRepository.existsByTickerAndTxidAndAddressIndex(
        ticker,
        txid,
        addressIndex
      )
    ) {
      return false;
    }

    DepositNoticeEntity notice = new DepositNoticeEntity(
      ticker,
      txid,
      addressIndex,
      userId,
      raw
    );
    notice.setId(new ObjectId().toHexString());
    try {
      depositNoticesRepository.insert(notice);
    } catch (DuplicateKeyException e) {
      return false;
    }
    return true;
  }
}
