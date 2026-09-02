package com.nanobot.nanobotbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.util.Constants;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "currencies")
public class CurrencyEntity extends BaseEntity {

  @Indexed(unique = true)
  private String ticker;

  private String name;

  private String address;

  private String nodeUrl;

  /**
   * Node websocket endpoint for confirmation notifications, for example
   * {@code ws://nano-node:7078}.
   *
   * <p>Left unset the currency falls back to polling the node on every cron
   * tick, which is what every Nano fork did before push notifications existed.
   * Treating null as "keep the old behaviour" is what lets this be enabled one
   * currency at a time rather than everywhere at once.
   */
  private String websocketUrl;

  private String protocol;

  // GET /currencies serializes this entity directly rather than a DTO, and the
  // Discord frontend polls that endpoint. These three gate a spend-capable
  // wallet RPC, so they must never appear in a response body. Mongo mapping uses
  // field access and is unaffected; the controller only accepts CurrencyDto on
  // writes, so ignoring these for deserialization costs nothing.
  @JsonIgnore
  private String walletRpcUrl;

  @JsonIgnore
  private String walletRpcUser;

  @JsonIgnore
  private String walletRpcPassword;

  private boolean enabled;

  private boolean processDeposits;

  private boolean processWithdrawals;

  private String openDifficulty;

  private String receiveDifficulty;

  private String sendDifficulty;

  private String updateDifficulty;

  private String emoji;

  private String color;

  private String precision;

  private String liquidity;

  private String value;

  private String minimumDeposit;

  private String minimumWithdraw;

  private String minimumDrop;

  private String minimumGift;

  private String minimumRain;

  private String confirmations;

  private String lastScannedHeight;

  private String priceId;

  private String explorerAccountUrl;

  private String explorerTxUrl;

  private String addressFormat;

  /**
   * Current network fee estimate in raw units, refreshed from the chain.
   *
   * <p>Deliberately separate from minimumWithdraw rather than folded into it.
   * Adding the fee to the stored minimum on every refresh would compound, so the
   * minimum would climb without bound. Keeping them apart preserves the
   * configured policy floor, and the effective minimum is the sum of the two.
   */
  private String feeEstimate;

  /** Monero transaction priority, 1 (low) through 4 (highest). */
  private String feePriority;

  /**
   * Whether holdings of this currency are withheld from the public audit.
   *
   * <p>Set for privacy coins, where publishing exact balances works against the
   * point of the chain. A privileged audit still shows the real figures.
   */
  private Boolean concealBalances;

  private Boolean supportsRepresentative;

  public CurrencyEntity() {
    super();
  }

  public CurrencyEntity(CurrencyDto dto) {
    super(dto.getId());
    this.ticker = dto.getTicker();
    this.name = dto.getName();
    this.address = dto.getAddress();
    this.nodeUrl = dto.getNodeUrl();
    this.websocketUrl = dto.getWebsocketUrl();
    this.protocol = dto.getProtocol();
    this.enabled = dto.getEnabled();
    this.processDeposits = dto.getProcessDeposits();
    this.processWithdrawals = dto.getProcessWithdrawals();
    this.openDifficulty = dto.getOpenDifficulty();
    this.receiveDifficulty = dto.getReceiveDifficulty();
    this.sendDifficulty = dto.getSendDifficulty();
    this.updateDifficulty = dto.getUpdateDifficulty();
    this.emoji = dto.getEmoji();
    this.color = dto.getColor();
    this.precision = dto.getPrecision();
    this.liquidity = dto.getLiquidity();
    this.value = dto.getValue();
    this.minimumDeposit = dto.getMinimumDeposit();
    this.minimumWithdraw = dto.getMinimumWithdraw();
    this.minimumDrop = dto.getMinimumDrop();
    this.minimumGift = dto.getMinimumGift();
    this.minimumRain = dto.getMinimumRain();
    this.confirmations = dto.getConfirmations();
    this.explorerAccountUrl = dto.getExplorerAccountUrl();
    this.explorerTxUrl = dto.getExplorerTxUrl();
    this.addressFormat = dto.getAddressFormat();
    this.feeEstimate = dto.getFeeEstimate();
    this.feePriority = dto.getFeePriority();
    this.concealBalances = dto.getConcealBalances();
    this.supportsRepresentative = dto.getSupportsRepresentative();
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getNodeUrl() {
    return nodeUrl;
  }

  public void setNodeUrl(String nodeUrl) {
    this.nodeUrl = nodeUrl;
  }

  public String getWebsocketUrl() {
    return websocketUrl;
  }

  public void setWebsocketUrl(String websocketUrl) {
    this.websocketUrl = websocketUrl;
  }

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean getProcessDeposits() {
    return processDeposits;
  }

  public void setProcessDeposits(boolean processDeposits) {
    this.processDeposits = processDeposits;
  }

  public boolean getProcessWithdrawals() {
    return processWithdrawals;
  }

  public void setProcessWithdrawals(boolean processWithdrawals) {
    this.processWithdrawals = processWithdrawals;
  }

  public String getOpenDifficulty() {
    return openDifficulty;
  }

  public void setOpenDifficulty(String openDifficulty) {
    this.openDifficulty = openDifficulty;
  }

  public String getReceiveDifficulty() {
    return receiveDifficulty;
  }

  public void setReceiveDifficulty(String receiveDifficulty) {
    this.receiveDifficulty = receiveDifficulty;
  }

  public String getSendDifficulty() {
    return sendDifficulty;
  }

  public void setSendDifficulty(String sendDifficulty) {
    this.sendDifficulty = sendDifficulty;
  }

  public String getUpdateDifficulty() {
    return updateDifficulty;
  }

  public void setUpdateDifficulty(String updateDifficulty) {
    this.updateDifficulty = updateDifficulty;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getPrecision() {
    return precision;
  }

  public void setPrecision(String precision) {
    this.precision = precision;
  }

  public String getLiquidity() {
    return liquidity;
  }

  public void setLiquidity(String liquidity) {
    this.liquidity = liquidity;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getMinimumDeposit() {
    return minimumDeposit;
  }

  public void setMinimumDeposit(String minimumDeposit) {
    this.minimumDeposit = minimumDeposit;
  }

  public String getMinimumWithdraw() {
    return minimumWithdraw;
  }

  public void setMinimumWithdraw(String minimumWithdraw) {
    this.minimumWithdraw = minimumWithdraw;
  }

  public String getMinimumDrop() {
    return minimumDrop;
  }

  public void setMinimumDrop(String minimumDrop) {
    this.minimumDrop = minimumDrop;
  }

  public String getMinimumGift() {
    return minimumGift;
  }

  public void setMinimumGift(String minimumGift) {
    this.minimumGift = minimumGift;
  }

  public String getMinimumRain() {
    return minimumRain;
  }

  public void setMinimumRain(String minimumRain) {
    this.minimumRain = minimumRain;
  }

  /**
   * Resolves to NANO when unset. Currency documents created before Monero
   * support existed have no protocol field, and they are all Nano forks.
   */
  public String getProtocol() {
    return (protocol == null || protocol.isBlank())
      ? Constants.PROTOCOL_NANO
      : protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getWalletRpcUrl() {
    return walletRpcUrl;
  }

  public void setWalletRpcUrl(String walletRpcUrl) {
    this.walletRpcUrl = walletRpcUrl;
  }

  public String getWalletRpcUser() {
    return walletRpcUser;
  }

  public void setWalletRpcUser(String walletRpcUser) {
    this.walletRpcUser = walletRpcUser;
  }

  public String getWalletRpcPassword() {
    return walletRpcPassword;
  }

  public void setWalletRpcPassword(String walletRpcPassword) {
    this.walletRpcPassword = walletRpcPassword;
  }

  public String getConfirmations() {
    return confirmations;
  }

  public void setConfirmations(String confirmations) {
    this.confirmations = confirmations;
  }

  public String getLastScannedHeight() {
    return lastScannedHeight;
  }

  public void setLastScannedHeight(String lastScannedHeight) {
    this.lastScannedHeight = lastScannedHeight;
  }

  public String getPriceId() {
    return priceId;
  }

  public void setPriceId(String priceId) {
    this.priceId = priceId;
  }

  public String getExplorerAccountUrl() {
    return explorerAccountUrl;
  }

  public void setExplorerAccountUrl(String explorerAccountUrl) {
    this.explorerAccountUrl = explorerAccountUrl;
  }

  public String getExplorerTxUrl() {
    return explorerTxUrl;
  }

  public void setExplorerTxUrl(String explorerTxUrl) {
    this.explorerTxUrl = explorerTxUrl;
  }

  public String getAddressFormat() {
    return addressFormat;
  }

  public void setAddressFormat(String addressFormat) {
    this.addressFormat = addressFormat;
  }

  public String getFeeEstimate() {
    return feeEstimate;
  }

  public void setFeeEstimate(String feeEstimate) {
    this.feeEstimate = feeEstimate;
  }

  public String getFeePriority() {
    return feePriority;
  }

  public void setFeePriority(String feePriority) {
    this.feePriority = feePriority;
  }

  /** Defaults to false so existing currencies stay publicly auditable. */
  public Boolean getConcealBalances() {
    return concealBalances == null ? Boolean.FALSE : concealBalances;
  }

  public void setConcealBalances(Boolean concealBalances) {
    this.concealBalances = concealBalances;
  }

  /**
   * Resolves to true when unset. Nano and Banano documents predate this field
   * and do support representatives, so a missing value must not disable
   * /update for them.
   */
  public Boolean getSupportsRepresentative() {
    return supportsRepresentative == null
      ? Boolean.TRUE
      : supportsRepresentative;
  }

  public void setSupportsRepresentative(Boolean supportsRepresentative) {
    this.supportsRepresentative = supportsRepresentative;
  }
}
