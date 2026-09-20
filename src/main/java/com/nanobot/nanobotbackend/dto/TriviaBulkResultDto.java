package com.nanobot.nanobotbackend.dto;

import java.util.ArrayList;
import java.util.List;

/** Outcome of a bulk trivia import: what went in, what was skipped, and why. */
public class TriviaBulkResultDto {

  private int received;

  private int inserted;

  private int duplicates;

  private List<String> rejected;

  public TriviaBulkResultDto() {
    this.rejected = new ArrayList<>();
  }

  public int getReceived() {
    return received;
  }

  public void setReceived(int received) {
    this.received = received;
  }

  public int getInserted() {
    return inserted;
  }

  public void setInserted(int inserted) {
    this.inserted = inserted;
  }

  public int getDuplicates() {
    return duplicates;
  }

  public void setDuplicates(int duplicates) {
    this.duplicates = duplicates;
  }

  public List<String> getRejected() {
    return rejected;
  }

  public void setRejected(List<String> rejected) {
    this.rejected = rejected;
  }
}
