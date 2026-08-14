package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "transactions")
public class TransactionEntity extends BaseEntity {

  private String blockHash;

  private String guildId;

  private String channelId;

  private String primaryUserId;

  private String secondaryUserId;

  private List<String> primaryReceiverIds;

  private List<String> secondaryReceiverIds;

  private String command;

  private Map<String, TransferDto> completedPrimaryTransfers = Map.of();

  private Map<String, TransferDto> completedSecondaryTransfers = Map.of();

  private Map<String, TransferDto> completedPrimaryGuildTransfers = Map.of();

  private Map<String, TransferDto> completedSecondaryGuildTransfers = Map.of();

  private Set<GuildWalletsDto> guildWalletHistories = Set.of();

  private Set<GuildWalletsEntity> guildWalletQuantities = Set.of();

  private String input;

  private TransferDto primaryTransfer;

  private TransferDto secondaryTransfer;

  private Set<UserWalletsDto> userWalletHistories = Set.of();

  private Set<UserItemsDto> userItemsHistories = Set.of();

  private Set<UserWalletsEntity> userWalletQuantities = Set.of();

  private Set<UserItemsEntity> userItemsQuantities = Set.of();

  private Date timestamp;

  public TransactionEntity() {
    super();
  }

  public TransactionEntity(TransactionDto transaction) {
    super(transaction.getId());
    this.blockHash = transaction.getBlockHash();
    this.guildId = transaction.getGuildId();
    this.channelId = transaction.getChannelId();
    this.primaryUserId = transaction.getPrimaryUserId();
    this.secondaryUserId = transaction.getSecondaryUserId();
    this.primaryReceiverIds = transaction.getPrimaryReceiverIds();
    this.secondaryReceiverIds = transaction.getSecondaryReceiverIds();
    this.command = transaction.getCommand();
    this.completedPrimaryTransfers = transaction.getCompletedPrimaryTransfers();
    this.completedSecondaryTransfers =
      transaction.getCompletedSecondaryTransfers();

    this.completedPrimaryGuildTransfers =
      transaction.getCompletedPrimaryGuildTransfers();
    this.completedSecondaryGuildTransfers =
      transaction.getCompletedSecondaryGuildTransfers();
    this.guildWalletHistories = transaction.getGuildWalletHistories();
    this.guildWalletQuantities = transaction.getGuildWalletQuantities();
    this.input = transaction.getInput();
    this.primaryTransfer = transaction.getPrimaryTransfer();
    this.secondaryTransfer = transaction.getSecondaryTransfer();
    this.userWalletHistories = transaction.getUserWalletHistories();
    this.userItemsHistories = transaction.getUserItemHistories();
    this.userWalletQuantities = transaction.getUserWalletQuantities();
    this.userItemsQuantities = transaction.getUserItemQuantities();
    this.timestamp = transaction.getTimestamp();
  }

  public String getBlockHash() {
    return blockHash;
  }

  public void setBlockHash(String blockHash) {
    this.blockHash = blockHash;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getPrimaryUserId() {
    return primaryUserId;
  }

  public void setPrimaryUserId(String primaryUserId) {
    this.primaryUserId = primaryUserId;
  }

  public String getSecondaryUserId() {
    return secondaryUserId;
  }

  public void setSecondaryUserId(String secondaryUserId) {
    this.secondaryUserId = secondaryUserId;
  }

  public List<String> getPrimaryReceiverIds() {
    return primaryReceiverIds;
  }

  public void setPrimaryReceiverIds(List<String> primaryReceiverIds) {
    this.primaryReceiverIds = primaryReceiverIds;
  }

  public List<String> getSecondaryReceiverIds() {
    return secondaryReceiverIds;
  }

  public void setSecondaryReceiverIds(List<String> secondaryReceiverIds) {
    this.secondaryReceiverIds = secondaryReceiverIds;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public Map<String, TransferDto> getCompletedPrimaryTransfers() {
    return completedPrimaryTransfers;
  }

  public void setCompletedPrimaryTransfers(
    Map<String, TransferDto> completedPrimaryTransfers
  ) {
    this.completedPrimaryTransfers = completedPrimaryTransfers;
  }

  public Map<String, TransferDto> getCompletedSecondaryTransfers() {
    return completedSecondaryTransfers;
  }

  public void setCompletedSecondaryTransfers(
    Map<String, TransferDto> completedSecondaryTransfers
  ) {
    this.completedSecondaryTransfers = completedSecondaryTransfers;
  }

  public Map<String, TransferDto> getCompletedPrimaryGuildTransfers() {
    return completedPrimaryGuildTransfers;
  }

  public void setCompletedPrimaryGuildTransfers(
    Map<String, TransferDto> completedPrimaryGuildTransfers
  ) {
    this.completedPrimaryGuildTransfers = completedPrimaryGuildTransfers;
  }

  public Map<String, TransferDto> getCompletedSecondaryGuildTransfers() {
    return completedSecondaryGuildTransfers;
  }

  public void setCompletedSecondaryGuildTransfers(
    Map<String, TransferDto> completedSecondaryGuildTransfers
  ) {
    this.completedSecondaryGuildTransfers = completedSecondaryGuildTransfers;
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

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
