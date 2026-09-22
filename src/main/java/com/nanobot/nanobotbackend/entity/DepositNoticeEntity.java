package com.nanobot.nanobotbackend.entity;

import java.util.Date;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The incoming-deposit notice lifecycle for one (ticker, txid, addressIndex).
 *
 * <p>A row means the deposit has been announced as discovered. {@code
 * confirmedAt} set means the follow-up "credited" notice has gone out too.
 * Both flags are write-once so a scan can re-see the same transaction forever
 * without repeating either message.
 *
 * <p>Deliberately separate from {@code depositRecords}. That collection is the
 * exactly-once guard for crediting, and a row there means money moved; mixing
 * in rows that only mean "we mentioned it" would force every credit path to
 * distinguish the two, and a mistake there pays someone twice. Keeping the
 * notices apart means the credit guard is untouched by this feature.
 *
 * <p>Keyed the same way as a deposit record, on (ticker, txid, addressIndex),
 * because that is what identifies one deposit to one user. The unique index is
 * what makes the insert-or-skip pattern safe against a repeated scan.
 *
 * <p>Rows expire after {@link #TTL_SECONDS}. Once a deposit has been credited
 * it never again appears with too few confirmations, so the notice has nothing
 * left to guard against, and letting Mongo drop it keeps the collection from
 * growing forever. Fourteen days matches Bitcoin Core's default mempool
 * expiry, so a stuck 0-conf cannot be announced a second time while the
 * network still holds it.
 */
@Document(collection = "depositNotices")
@CompoundIndex(
  name = "ticker_txid_index_unique",
  def = "{'ticker': 1, 'txid': 1, 'addressIndex': 1}",
  unique = true
)
public class DepositNoticeEntity extends BaseEntity {

  /** Fourteen days: Bitcoin Core drops unconfirmed transactions at this age. */
  public static final int TTL_SECONDS = 14 * 24 * 60 * 60;

  private String ticker;

  private String txid;

  private Long addressIndex;

  private String userId;

  private String raw;

  @Indexed(name = "timestamp_ttl", expireAfter = "14d")
  private Date timestamp;

  /** Set once the Deposit Confirmed notice has been written. */
  private Date confirmedAt;

  public DepositNoticeEntity() {
    super();
  }

  public DepositNoticeEntity(
    String ticker,
    String txid,
    Long addressIndex,
    String userId,
    String raw
  ) {
    super();
    this.ticker = ticker;
    this.txid = txid;
    this.addressIndex = addressIndex;
    this.userId = userId;
    this.raw = raw;
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

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Date getConfirmedAt() {
    return confirmedAt;
  }

  public void setConfirmedAt(Date confirmedAt) {
    this.confirmedAt = confirmedAt;
  }
}
