package com.nanobot.nanobotbackend.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StringUtilTest {

  @Test
  void isValidStringShouldReturnTrueForValidString() {
    assertTrue(StringUtil.isValidString("hello"));
    assertTrue(StringUtil.isValidString("valid"));
    assertTrue(StringUtil.isValidString("a"));
  }

  @Test
  void isValidStringShouldReturnFalseForNull() {
    assertFalse(StringUtil.isValidString(null));
  }

  @Test
  void isValidStringShouldReturnFalseForEmptyString() {
    assertFalse(StringUtil.isValidString(""));
  }

  @Test
  void isValidStringShouldReturnFalseForSingleSpace() {
    assertFalse(StringUtil.isValidString(" "));
  }

  @Test
  void isValidStringShouldReturnFalseForNullStringLiteral() {
    assertFalse(StringUtil.isValidString("null"));
  }

  @Test
  void isValidStringShouldReturnFalseForNullStringLiteralCaseInsensitive() {
    assertFalse(StringUtil.isValidString("NULL"));
    assertFalse(StringUtil.isValidString("Null"));
  }

  @Test
  void isValidStringShouldReturnTrueForStringContainingNull() {
    assertTrue(StringUtil.isValidString("nullable"));
  }

  @Test
  void isValidStringShouldReturnTrueForStringWithLeadingTrailingSpaces() {
    assertTrue(StringUtil.isValidString("  valid  "));
  }

  @Test
  void isValidStringShouldReturnTrueForMultipleSpacesAsContent() {
    assertTrue(StringUtil.isValidString("   "));
  }

  @Test
  void isValidStringShouldReturnTrueForStringWithTabAndContent() {
    assertTrue(StringUtil.isValidString("a\tb"));
  }

  @Test
  void isValidStringShouldReturnTrueForUnicodeContent() {
    assertTrue(StringUtil.isValidString("日本語"));
    assertTrue(StringUtil.isValidString("emoji 🐟"));
  }
}
