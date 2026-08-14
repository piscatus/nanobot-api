package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "currencies")
public class CurrencyEntity extends BaseEntity {

  @Indexed(unique = true)
  private String ticker;

  private String name;

  private String address;

  private String nodeUrl;

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

  public CurrencyEntity() {
    super();
  }

  public CurrencyEntity(CurrencyDto dto) {
    super(dto.getId());
    this.ticker = dto.getTicker();
    this.name = dto.getName();
    this.address = dto.getAddress();
    this.nodeUrl = dto.getNodeUrl();
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
}
