package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.MessageDto;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
public class MessageEntity extends BaseEntity {

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

  public MessageEntity() {
    super();
  }

  public MessageEntity(MessageDto message) {
    super(message.getId());
    this.userId = message.getUserId();
    this.guildId = message.getGuildId();
    this.channelId = message.getChannelId();
    this.messageId = message.getMessageId();
    this.title = message.getTitle();
    this.color = message.getColor();
    this.content = message.getContent();
    this.footer = message.getFooter();
    this.timestamp = message.getTimestamp();
    this.list = message.getList();
    this.url = message.getUrl();
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

  public List<Map<String, Object>> getList() {
    return list;
  }

  public void setList(List<Map<String, Object>> list) {
    this.list = list;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
