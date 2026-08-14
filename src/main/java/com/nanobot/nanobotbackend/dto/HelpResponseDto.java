package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class HelpResponseDto extends BaseResponseDto {

  private List<CurrencyDto> currencies;

  public HelpResponseDto() {
    super();
  }

  public HelpResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }
}
