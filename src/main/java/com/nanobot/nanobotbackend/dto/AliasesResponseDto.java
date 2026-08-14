package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class AliasesResponseDto extends BaseResponseDto {

  private List<AliasDto> aliases;

  private List<CurrencyDto> currencies;

  public AliasesResponseDto() {
    super();
  }

  public AliasesResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<AliasDto> getAliases() {
    return aliases;
  }

  public void setAliases(List<AliasDto> aliases) {
    this.aliases = aliases;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }
}
