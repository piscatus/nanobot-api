package com.nanobot.nanobotbackend.dto;

import java.util.Date;

public class ItemDto extends BaseDto {

  private String name;

  private int quantity;

  private boolean required;

  private Date timestamp;

  public ItemDto() {
    super();
  }

  public ItemDto(ItemDto dto) {
    super(dto.getId());
    this.name = dto.getName();
    this.quantity = dto.getQuantity();
    this.required = dto.getRequired();
  }

  public ItemDto(String name, int quantity) {
    super();
    this.name = name;
    this.quantity = quantity;
  }

  public ItemDto(String name, int quantity, boolean required) {
    super();
    this.name = name;
    this.quantity = quantity;
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public boolean getRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
