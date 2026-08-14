package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class ReceiveResponseDto extends BaseResponseDto {

  private List<WalletDto> addresses;

  private List<CurrencyDto> currencies;

  public ReceiveResponseDto() {
    super();
  }

  public ReceiveResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<WalletDto> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<WalletDto> addresses) {
    this.addresses = addresses;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }
}
