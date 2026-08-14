package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.ProfanityDto;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "profanities")
public class ProfanityEntity extends BaseEntity {

  @Indexed(unique = true)
  private String singular;

  @Indexed(unique = true)
  private String plural;

  private boolean contains;

  public ProfanityEntity() {
    super();
  }

  public ProfanityEntity(ProfanityDto profanity) {
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
