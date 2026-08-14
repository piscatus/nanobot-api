package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class ConfigResponseDto extends BaseResponseDto {

  private List<CurrencyDto> currencies;

  private AliasDto aliasDetails;

  private List<CreatureDto> creatures;

  private TransferDto transfer;

  public ConfigResponseDto() {
    super();
  }

  public ConfigResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public List<CreatureDto> getCreatures() {
    return creatures;
  }

  public void setCreatures(List<CreatureDto> creatures) {
    this.creatures = creatures;
  }

  public AliasDto getAliasDetails() {
    return aliasDetails;
  }

  public void setAliasDetails(AliasDto aliasDetails) {
    this.aliasDetails = aliasDetails;
  }

  public TransferDto getTransfer() {
    return transfer;
  }

  public void setTransfer(TransferDto transfer) {
    this.transfer = transfer;
  }
}
