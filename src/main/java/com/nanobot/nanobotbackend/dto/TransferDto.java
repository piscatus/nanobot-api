package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import java.util.ArrayList;
import java.util.List;

public class TransferDto {

  private boolean pricey;
  private List<AliasDto> aliases;
  private List<WalletDto> wallets;
  private List<ItemDto> items;

  public TransferDto(List<WalletDto> wallets, List<ItemDto> items) {
    super();
    this.pricey = false;
    this.wallets = wallets;
    this.items = items;
  }

  public TransferDto() {
    super();
    this.pricey = false;
    this.wallets = new ArrayList<>();
    this.items = new ArrayList<>();
  }

  public boolean getPricey() {
    return pricey;
  }

  public void setPricey(boolean pricey) {
    this.pricey = pricey;
  }

  public List<AliasDto> getAliases() {
    return aliases;
  }

  public void setAliases(List<AliasDto> aliases) {
    this.aliases = aliases;
  }

  public List<WalletDto> getWallets() {
    return wallets;
  }

  public void setWallets(List<WalletDto> wallets) {
    this.wallets = wallets;
  }

  public List<ItemDto> getItems() {
    return items;
  }

  public void setItems(List<ItemDto> items) {
    this.items = items;
  }
}
