package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.MessageEntity;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageDto extends BaseDto {

  private String channelId;

  private String color;

  private String content;

  private String footer;

  private String guildId;

  private List<Map<String, Object>> list;

  private String messageId;

  private Date timestamp;

  private String title;

  private String url;

  private String userId;

  public MessageDto(MessageEntity entity) {
    super(entity.getId());
    this.channelId = entity.getChannelId();
    this.color = entity.getColor();
    this.content = entity.getContent();
    this.footer = entity.getFooter();
    this.guildId = entity.getGuildId();
    this.messageId = entity.getMessageId();
    this.timestamp = entity.getTimestamp();
    this.title = entity.getTitle();
    this.url = entity.getUrl();
    this.userId = entity.getUserId();
  }

  public MessageDto(
    String userId,
    String guildId,
    String channelId,
    String messageId,
    String title,
    String color,
    String content,
    Date timestamp,
    String url,
    List<Map<String, Object>> list,
    String footer
  ) {
    super();
    this.userId = userId;
    this.guildId = guildId;
    this.channelId = channelId;
    this.messageId = messageId;
    this.title = title;
    this.color = color;
    this.content = content;
    this.timestamp = timestamp;
    this.url = url;
    this.list = list;
    this.footer = footer;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getFooter() {
    return footer;
  }

  public void setFooter(String footer) {
    this.footer = footer;
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

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<Map<String, Object>> getList() {
    return list;
  }

  public void setList(List<Map<String, Object>> list) {
    this.list = list;
  }
}
