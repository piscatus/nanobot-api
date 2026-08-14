package com.nanobot.nanobotbackend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeUtilTest {

  @Test
  void formatTimeRemainingShouldFormatDaysHoursMinutesSeconds() {
    long ms = 2 * 86400000L + 3 * 3600000L + 15 * 60000L + 30 * 1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("2 days, 3 hours, 15 minutes and 30 seconds", result);
  }

  @Test
  void formatTimeRemainingShouldUseSingularForOneDay() {
    long ms = 86400000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 day", result);
  }

  @Test
  void formatTimeRemainingShouldUsePluralForMultipleDays() {
    long ms = 3 * 86400000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("3 days", result);
  }

  @Test
  void formatTimeRemainingShouldFormatHoursOnly() {
    long ms = 5 * 3600000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("5 hours", result);
  }

  @Test
  void formatTimeRemainingShouldFormatMinutesOnly() {
    long ms = 45 * 60000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("45 minutes", result);
  }

  @Test
  void formatTimeRemainingShouldFormatSecondsOnly() {
    long ms = 10 * 1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("10 seconds", result);
  }

  @Test
  void formatTimeRemainingShouldFormatSingleSecond() {
    long ms = 1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 second", result);
  }

  @Test
  void formatTimeRemainingShouldHandleZeroAsOneSecond() {
    long ms = 0L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 second", result);
  }

  @Test
  void formatTimeRemainingShouldFormatDaysAndSeconds() {
    long ms = 86400000L + 5 * 1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 day, 5 seconds", result);
  }

  @Test
  void formatTimeRemainingShouldReturnEmptyForNegativeInput() {
    long ms = -1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("", result);
  }

  @Test
  void formatTimeRemainingShouldFormatMinutesAndSeconds() {
    long ms = 2 * 60000L + 3 * 1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("2 minutes and 3 seconds", result);
  }

  @Test
  void formatTimeRemainingShouldUseSingularForOneHour() {
    long ms = 3600000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 hour", result);
  }

  @Test
  void formatTimeRemainingShouldUseSingularForOneMinute() {
    long ms = 60000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 minute", result);
  }

  @Test
  void formatTimeRemainingShouldFormatHoursMinutesAndSeconds() {
    long ms = 3600000L + 2 * 60000L + 5 * 1000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 hour, 2 minutes and 5 seconds", result);
  }

  @Test
  void formatTimeRemainingShouldFormatDaysHoursAndMinutes() {
    long ms = 86400000L + 2 * 3600000L + 30 * 60000L;
    String result = TimeUtil.formatTimeRemaining(ms);
    assertEquals("1 day, 2 hours, 30 minutes", result);
  }
}
