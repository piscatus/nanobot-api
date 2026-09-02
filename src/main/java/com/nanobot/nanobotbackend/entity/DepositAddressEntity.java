package com.nanobot.nanobotbackend.entity;

import java.util.Date;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A deposit address allocated to a user for one currency.
 *
 * <p>Nano and Banano do not need this: their addresses are derived from the
 * user's own seed and can be recomputed at any time. Monero cannot work that
 * way, because a wallet per user would mean a chain scan per user. Instead one
 * hot wallet issues a subaddress per user, and the wallet only identifies
 * incoming funds by subaddress index, so the index to user mapping has to be
 * stored to know who a deposit belongs to.
 */
@Document(collection = "depositAddresses")
@CompoundIndexes(
  {
    @CompoundIndex(
      name = "ticker_userId_unique",
      def = "{'ticker': 1, 'userId': 1}",
      unique = true
    ),
    @CompoundIndex(
      name = "ticker_address_unique",
      def = "{'ticker': 1, 'address': 1}",
      unique = true
    ),
    @CompoundIndex(
      name = "ticker_addressIndex_unique",
      def = "{'ticker': 1, 'addressIndex': 1}",
      unique = true
    ),
  }
)
public class DepositAddressEntity extends BaseEntity {

  private String userId;

  private String ticker;

  private String address;

  /** Subaddress index within the hot wallet account. */
  private Long addressIndex;

  private Date timestamp;

  public DepositAddressEntity() {
    super();
  }

  public DepositAddressEntity(
    String userId,
    String ticker,
    String address,
    Long addressIndex
  ) {
    super();
    this.userId = userId;
    this.ticker = ticker;
    this.address = address;
    this.addressIndex = addressIndex;
    this.timestamp = new Date();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public Long getAddressIndex() {
    return addressIndex;
  }

  public void setAddressIndex(Long addressIndex) {
    this.addressIndex = addressIndex;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
