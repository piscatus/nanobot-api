package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.Date;

public class QueueDto extends BaseDto {

  private String userId;

  private String sourceAddress;

  private String targetAddress;

  private LevelDto level;

  private String blockHash;

  /** For a RECEIVE, the hash of the incoming send being pocketed. */
  private String sourceHash;

  private String raw;

  private String ticker;

  private boolean processed;

  private String seed;

  private Long index;

  private String privateKey;

  private Date timestamp;

  private String transactionId;

  private Integer attempts;

  public QueueDto(QueueEntity entity) {
    super(entity.getId());
    this.userId = entity.getUserId();
    this.sourceAddress = entity.getSourceAddress();
    this.targetAddress = entity.getTargetAddress();
    this.level = entity.getLevel();
    this.blockHash = entity.getBlockHash();
    this.sourceHash = entity.getSourceHash();
    this.raw = entity.getRaw();
    this.ticker = entity.getTicker();
    this.processed = entity.getProcessed();
    this.seed = entity.getSeed();
    this.index = entity.getIndex();
    this.privateKey = entity.getPrivateKey();
    this.timestamp = entity.getTimestamp();
    this.transactionId = entity.getTransactionId();
    this.attempts = entity.getAttempts();
  }

  public Integer getAttempts() {
    return attempts;
  }

  public void setAttempts(Integer attempts) {
    this.attempts = attempts;
  }

  public QueueDto(
    String userId,
    String sourceAddress,
    String targetAddress,
    LevelDto level,
    String blockHash,
    String raw,
    String ticker,
    boolean processed,
    String seed,
    Date timestamp,
    String transactionId
  ) {
    super();
    this.userId = userId;
    this.sourceAddress = sourceAddress;
    this.targetAddress = targetAddress;
    this.level = level;
    this.blockHash = blockHash;
    this.sourceHash = level == LevelDto.RECEIVE ? blockHash : null;
    this.raw = raw;
    this.ticker = ticker;
    this.processed = processed;
    this.seed = seed;
    this.timestamp = timestamp;
    this.transactionId = transactionId;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSourceAddress() {
    return sourceAddress;
  }

  public void setSourceAddress(String sourceAddress) {
    this.sourceAddress = sourceAddress;
  }

  public String getTargetAddress() {
    return targetAddress;
  }

  public void setTargetAddress(String targetAddress) {
    this.targetAddress = targetAddress;
  }

  public LevelDto getLevel() {
    return level;
  }

  public void setLevel(LevelDto level) {
    this.level = level;
  }

  public String getBlockHash() {
    return blockHash;
  }

  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  public String getSourceHash() {
    return sourceHash;
  }

  public void setSourceHash(String sourceHash) {
    this.sourceHash = sourceHash;
  }

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public boolean getProcessed() {
    return processed;
  }

  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

  public String getSeed() {
    return seed;
  }

  public void setSeed(String seed) {
    this.seed = seed;
  }

  public Long getIndex() {
    return index;
  }

  public void setIndex(Long index) {
    this.index = index;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
