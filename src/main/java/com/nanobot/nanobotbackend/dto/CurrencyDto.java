package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;

public class CurrencyDto extends BaseDto {

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

  public CurrencyDto() {
    super();
  }

  public CurrencyDto(CurrencyEntity entity) {
    super(entity.getId());
    this.ticker = entity.getTicker();
    this.name = entity.getName();
    this.address = entity.getAddress();
    this.nodeUrl = entity.getNodeUrl();
    this.enabled = entity.getEnabled();
    this.processDeposits = entity.getProcessDeposits();
    this.processWithdrawals = entity.getProcessWithdrawals();
    this.openDifficulty = entity.getOpenDifficulty();
    this.receiveDifficulty = entity.getReceiveDifficulty();
    this.sendDifficulty = entity.getSendDifficulty();
    this.updateDifficulty = entity.getUpdateDifficulty();
    this.emoji = entity.getEmoji();
    this.color = entity.getColor();
    this.precision = entity.getPrecision();
    this.liquidity = entity.getLiquidity();
    this.value = entity.getValue();
    this.minimumDeposit = entity.getMinimumDeposit();
    this.minimumWithdraw = entity.getMinimumWithdraw();
    this.minimumDrop = entity.getMinimumDrop();
    this.minimumGift = entity.getMinimumGift();
    this.minimumRain = entity.getMinimumRain();
  }

  public CurrencyDto(
    String ticker,
    String name,
    String address,
    String nodeUrl,
    boolean enabled,
    boolean processDeposits,
    boolean processWithdrawals,
    String receiveDifficulty,
    String sendDifficulty,
    String emoji,
    String color,
    String precision,
    String liquidity,
    String value,
    String minimumDeposit,
    String minimumWithdraw,
    String minimumDrop,
    String minimumGift,
    String minimumRain
  ) {
    super();
    this.ticker = ticker;
    this.name = name;
    this.address = address;
    this.nodeUrl = nodeUrl;
    this.enabled = enabled;
    this.processDeposits = processDeposits;
    this.processWithdrawals = processWithdrawals;
    this.receiveDifficulty = receiveDifficulty;
    this.sendDifficulty = sendDifficulty;
    this.emoji = emoji;
    this.color = color;
    this.precision = precision;
    this.liquidity = liquidity;
    this.value = value;
    this.minimumDeposit = minimumDeposit;
    this.minimumWithdraw = minimumWithdraw;
    this.minimumDrop = minimumDrop;
    this.minimumGift = minimumGift;
    this.minimumRain = minimumRain;
  }

  public CurrencyDto(
    String ticker,
    String name,
    boolean enabled,
    String emoji,
    String precision
  ) {
    super();
    this.ticker = ticker;
    this.name = name;
    this.enabled = enabled;
    this.emoji = emoji;
    this.precision = precision;
  }

  public CurrencyDto(String ticker, String name, boolean enabled) {
    super();
    this.ticker = ticker;
    this.name = name;
    this.enabled = enabled;
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
