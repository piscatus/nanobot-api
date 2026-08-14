package com.nanobot.nanobotbackend.dto;

public class WalletDto extends BaseDto {

  private String ticker;

  private String address;

  private String raw;

  private boolean required;

  public WalletDto() {
    super();
  }

  public WalletDto(WalletDto dto) {
    super(dto.getId());
    this.ticker = dto.getTicker();
    this.raw = dto.getRaw();
    this.required = dto.getRequired();
  }

  public WalletDto(String ticker, String raw) {
    super();
    this.ticker = ticker;
    this.raw = raw;
  }

  public WalletDto(String ticker, String address, String raw) {
    super();
    this.ticker = ticker;
    this.address = address;
    this.raw = raw;
  }

  public WalletDto(String ticker, String raw, boolean required) {
    super();
    this.ticker = ticker;
    this.raw = raw;
    this.required = required;
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

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  public boolean getRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }
}
