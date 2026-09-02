package com.nanobot.nanobotbackend.entity;

import java.util.Date;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A deposit that has already been credited, kept permanently as a double-credit
 * guard.
 *
 * <p>Monero needs this because {@code get_transfers} keeps returning a transfer
 * forever, so re-scanning a height range would re-credit the user.
 *
 * <p>Nano used to get the same protection for free, since a received block
 * stops appearing in {@code accounts_receivable} and so is never offered twice.
 * Subscribing to the node's confirmation topic gave that up: the node repeats
 * confirmations for a hash and replays them across a reconnect, so a settled
 * deposit can be announced again long after it was credited.
 *
 * <p>The unique index is the real protection in both cases, since it makes a
 * duplicate insert fail even if two threads race.
 *
 * <p>For Monero one transaction can pay several subaddresses, which are
 * separate deposits to separate users, so the key includes the address index
 * rather than being the transaction id alone. Nano has no subaddresses and
 * records the account's HD index there instead, which keeps the key shape the
 * same without ever colliding, because a block hash identifies exactly one
 * deposit.
 */
@Document(collection = "depositRecords")
@CompoundIndex(
  name = "ticker_txid_index_unique",
  def = "{'ticker': 1, 'txid': 1, 'addressIndex': 1}",
  unique = true
)
public class DepositRecordEntity extends BaseEntity {

  private String ticker;

  private String txid;

  private Long addressIndex;

  private String userId;

  private String raw;

  private String height;

  private String transactionId;

  private Date timestamp;

  public DepositRecordEntity() {
    super();
  }

  public DepositRecordEntity(
    String ticker,
    String txid,
    Long addressIndex,
    String userId,
    String raw,
    String height
  ) {
    super();
    this.ticker = ticker;
    this.txid = txid;
    this.addressIndex = addressIndex;
    this.userId = userId;
    this.raw = raw;
    this.height = height;
    this.timestamp = new Date();
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public String getTxid() {
    return txid;
  }

  public void setTxid(String txid) {
    this.txid = txid;
  }

  public Long getAddressIndex() {
    return addressIndex;
  }

  public void setAddressIndex(Long addressIndex) {
    this.addressIndex = addressIndex;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  public String getHeight() {
    return height;
  }

  public void setHeight(String height) {
    this.height = height;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
