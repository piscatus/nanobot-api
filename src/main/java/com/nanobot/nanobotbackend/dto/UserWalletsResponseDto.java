package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class UserWalletsResponseDto extends BaseResponseDto {

  private List<WalletDto> userWallets;

  private List<WalletDto> subordinateWallets;

  private List<CurrencyDto> currencies;

  public UserWalletsResponseDto() {
    super();
  }

  public UserWalletsResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<WalletDto> getUserWallets() {
    return userWallets;
  }

  public void setUserWallets(List<WalletDto> userWallets) {
    this.userWallets = userWallets;
  }

  public List<WalletDto> getSubordinateWallets() {
    return subordinateWallets;
  }

  public void setSubordinateWallets(List<WalletDto> subordinateWallets) {
    this.subordinateWallets = subordinateWallets;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }
}
