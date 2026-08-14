package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class CurrenciesResponseDto extends BaseResponseDto {

  private List<CurrencyDto> currencies;

  public CurrenciesResponseDto() {
    super();
  }

  public CurrenciesResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }
}
