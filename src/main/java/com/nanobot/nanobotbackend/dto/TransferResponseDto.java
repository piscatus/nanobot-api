package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TransferResponseDto extends BaseResponseDto {

  private List<ActivityDto> activities;

  private String blockHash;

  private List<AliasDto> aliases;

  private List<ItemDto> bonuses;

  private Optional<Map<String, TransferDto>> completedPrimaryTransfers;

  private Optional<Map<String, TransferDto>> completedSecondaryTransfers;

  private Optional<Map<String, TransferDto>> completedPrimaryGuildTransfers;

  private Optional<Map<String, TransferDto>> completedSecondaryGuildTransfers;

  private boolean confirmation;

  private List<CreatureDto> creatures;

  private List<CurrencyDto> currencies;

  private DropDto drop;

  private Set<GuildWalletsDto> guildWalletHistories;

  private Set<GuildWalletsEntity> guildWalletQuantities;

  private String input;

  private TransferDto primaryTransfer;

  private TransferDto secondaryTransfer;

  private String transactionId;

  private Set<UserWalletsDto> userWalletHistories;

  private Set<UserItemsDto> userItemsHistories;

  private Set<UserWalletsEntity> userWalletQuantities;

  private Set<UserItemsEntity> userItemsQuantities;

  public TransferResponseDto() {
    super();
  }

  public TransferResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public String getBlockHash() {
    return blockHash;
  }

  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  public List<ActivityDto> getActivities() {
    return activities;
  }

  public void setActivities(List<ActivityDto> activities) {
    this.activities = activities;
  }

  public List<AliasDto> getAliases() {
    return aliases;
  }

  public void setAliases(List<AliasDto> aliases) {
    this.aliases = aliases;
  }

  public List<ItemDto> getBonuses() {
    return bonuses;
  }

  public void setBonuses(List<ItemDto> bonuses) {
    this.bonuses = bonuses;
  }

  public Optional<Map<String, TransferDto>> getCompletedPrimaryTransfers() {
    return completedPrimaryTransfers;
  }

  public void setCompletedPrimaryTransfers(
    Optional<Map<String, TransferDto>> completedPrimaryTransfers
  ) {
    this.completedPrimaryTransfers = completedPrimaryTransfers;
  }

  public Optional<Map<String, TransferDto>> getCompletedSecondaryTransfers() {
    return completedSecondaryTransfers;
  }

  public void setCompletedSecondaryTransfers(
    Optional<Map<String, TransferDto>> completedSecondaryTransfers
  ) {
    this.completedSecondaryTransfers = completedSecondaryTransfers;
  }

  public Optional<
    Map<String, TransferDto>
  > getCompletedPrimaryGuildTransfers() {
    return completedPrimaryGuildTransfers;
  }

  public void setCompletedPrimaryGuildTransfers(
    Optional<Map<String, TransferDto>> completedPrimaryGuildTransfers
  ) {
    this.completedPrimaryGuildTransfers = completedPrimaryGuildTransfers;
  }

  public Optional<
    Map<String, TransferDto>
  > getCompletedSecondaryGuildTransfers() {
    return completedSecondaryGuildTransfers;
  }

  public void setCompletedSecondaryGuildTransfers(
    Optional<Map<String, TransferDto>> completedSecondaryGuildTransfers
  ) {
    this.completedSecondaryGuildTransfers = completedSecondaryGuildTransfers;
  }

  public boolean getConfirmation() {
    return confirmation;
  }

  public void setConfirmation(boolean confirmation) {
    this.confirmation = confirmation;
  }

  public List<CreatureDto> getCreatures() {
    return creatures;
  }

  public void setCreatures(List<CreatureDto> creatures) {
    this.creatures = creatures;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public DropDto getDrop() {
    return drop;
  }

  public void setDrop(DropDto drop) {
    this.drop = drop;
  }

  public Set<GuildWalletsDto> getGuildWalletHistories() {
    return guildWalletHistories;
  }

  public void setGuildWalletHistories(
    Set<GuildWalletsDto> guildWalletHistories
  ) {
    this.guildWalletHistories = guildWalletHistories;
  }

  public Set<GuildWalletsEntity> getGuildWalletQuantities() {
    return guildWalletQuantities;
  }

  public void setGuildWalletQuantities(
    Set<GuildWalletsEntity> guildWalletQuantities
  ) {
    this.guildWalletQuantities = guildWalletQuantities;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public TransferDto getPrimaryTransfer() {
    return primaryTransfer;
  }

  public void setPrimaryTransfer(TransferDto primaryTransfer) {
    this.primaryTransfer = primaryTransfer;
  }

  public TransferDto getSecondaryTransfer() {
    return secondaryTransfer;
  }

  public void setSecondaryTransfer(TransferDto secondaryTransfer) {
    this.secondaryTransfer = secondaryTransfer;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public Set<UserWalletsDto> getUserWalletHistories() {
    return userWalletHistories;
  }

  public void setUserWalletHistories(Set<UserWalletsDto> userWalletHistories) {
    this.userWalletHistories = userWalletHistories;
  }

  public Set<UserItemsDto> getUserItemHistories() {
    return userItemsHistories;
  }

  public void setUserItemHistories(Set<UserItemsDto> userItemsHistories) {
    this.userItemsHistories = userItemsHistories;
  }

  public Set<UserWalletsEntity> getUserWalletQuantities() {
    return userWalletQuantities;
  }

  public void setUserWalletQuantities(
    Set<UserWalletsEntity> userWalletQuantities
  ) {
    this.userWalletQuantities = userWalletQuantities;
  }

  public Set<UserItemsEntity> getUserItemQuantities() {
    return userItemsQuantities;
  }

  public void setUserItemQuantities(Set<UserItemsEntity> userItemsQuantities) {
    this.userItemsQuantities = userItemsQuantities;
  }
}
