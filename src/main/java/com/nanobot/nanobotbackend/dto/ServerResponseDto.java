package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class ServerResponseDto extends BaseResponseDto {

  private List<CurrencyDto> currencies;

  private List<WalletDto> guildWallets;

  public ServerResponseDto() {
    super();
  }

  public ServerResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public List<WalletDto> getGuildWallets() {
    return guildWallets;
  }

  public void setGuildWallets(List<WalletDto> guildWallets) {
    this.guildWallets = guildWallets;
  }
}
