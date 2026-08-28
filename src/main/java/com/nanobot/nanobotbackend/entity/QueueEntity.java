package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import java.util.Date;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "queues")
public class QueueEntity extends BaseEntity {

  private String userId;

  private String sourceAddress;

  private String targetAddress;

  private LevelDto level;

  private String blockHash;

  private String raw;

  private String ticker;

  private boolean processed;

  private String seed;

  private Long index;

  private String privateKey;

  private Date timestamp;

  private String transactionId;

  public QueueEntity() {
    super();
  }

  public QueueEntity(QueueDto queue) {
    super(queue.getId());
    this.userId = queue.getUserId();
    this.sourceAddress = queue.getSourceAddress();
    this.targetAddress = queue.getTargetAddress();
    this.level = queue.getLevel();
    this.blockHash = queue.getBlockHash();
    this.raw = queue.getRaw();
    this.ticker = queue.getTicker();
    this.processed = queue.getProcessed();
    this.seed = queue.getSeed();
    this.index = queue.getIndex();
    this.privateKey = queue.getPrivateKey();
    this.timestamp = queue.getTimestamp();
    this.transactionId = queue.getTransactionId();
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
