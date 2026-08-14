package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.ProfanityEntity;

public class ProfanityDto extends BaseDto {

  private String singular;

  private String plural;

  private boolean contains;

  public ProfanityDto() {
    super();
  }

  public ProfanityDto(ProfanityEntity profanity) {
    super(profanity.getId());
    this.singular = profanity.getSingular();
    this.plural = profanity.getPlural();
    this.contains = profanity.getContains();
  }

  public String getSingular() {
    return singular;
  }

  public void setSingular(String singular) {
    this.singular = singular;
  }

  public String getPlural() {
    return plural;
  }

  public void setPlural(String plural) {
    this.plural = plural;
  }

  public boolean getContains() {
    return contains;
  }

  public void setContains(boolean contains) {
    this.contains = contains;
  }
}
