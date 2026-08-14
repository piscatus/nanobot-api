package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class AuditResponseDto extends BaseResponseDto {

  private List<ItemDto> bonuses;

  private List<CurrencyDto> currencies;

  private List<CreatureDto> creatures;

  private List<DropDto> drops;

  private List<GuildWalletsDto> guildsWallets;

  private List<UserItemsDto> usersItems;

  private List<UserWalletsDto> usersWallets;

  public AuditResponseDto() {
    super();
  }

  public AuditResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<ItemDto> getBonuses() {
    return bonuses;
  }

  public void setBonuses(List<ItemDto> bonuses) {
    this.bonuses = bonuses;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCreatures(List<CreatureDto> creatures) {
    this.creatures = creatures;
  }

  public List<CreatureDto> getCreatures() {
    return creatures;
  }

  public List<DropDto> getDrops() {
    return drops;
  }

  public void setDrops(List<DropDto> drops) {
    this.drops = drops;
  }

  public List<UserItemsDto> getUsersItems() {
    return usersItems;
  }

  public void setUsersItems(List<UserItemsDto> usersItems) {
    this.usersItems = usersItems;
  }

  public List<UserWalletsDto> getUsersWallets() {
    return usersWallets;
  }

  public void setUsersWallets(List<UserWalletsDto> usersWallets) {
    this.usersWallets = usersWallets;
  }

  public List<GuildWalletsDto> getGuildsWallets() {
    return guildsWallets;
  }

  public void setGuildsWallets(List<GuildWalletsDto> guildsWallets) {
    this.guildsWallets = guildsWallets;
  }
}
