package com.nanobot.nanobotbackend.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class NumberUtilTest {

    @Test
    void testValidInteger() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("123", "test", 2, result::set, false);
        assertTrue(isValid);
        assertEquals("123", result.get());
    }

    @Test
    void testValidDecimalWithinPrecision() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("12.34", "test", 2, result::set, false);
        assertTrue(isValid);
        assertEquals("12.34", result.get());
    }

    @Test
    void testValidAllKeyword() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("all", "test", 3, result::set, false);
        assertTrue(isValid);
        assertEquals("0", result.get());
    }

    @Test
    void testInvalidNullInput() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber(null, "test", 2, result::set, false);
        assertFalse(isValid);
        assertEquals("Invalid Input: Quantity of null is invalid!", result.get());
    }

    @Test
    void testInvalidZero() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("0", "test", 2, result::set, false);
        assertFalse(isValid);
        assertEquals("Invalid Input: Quantity of zero is invalid!", result.get());
    }

    @Test
    void testInvalidNonNumericString() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("abc", "aliases", 2, result::set, false);
        assertFalse(isValid);
        assertEquals("Invalid Input: Quantity of abc is invalid!", result.get());
    }

    @Test
    void testTooManyDecimalPlaces() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("3.14159", "pi", 2, result::set, false);
        assertFalse(isValid);
        assertEquals("Invalid Input: The number specified for pi cannot exceed 2 decimal places.", result.get());
    }

    @Test
    void testDecimalExactlyAtPrecision() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("5.67", "value", 2, result::set, false);
        assertTrue(isValid);
        assertEquals("5.67", result.get());
    }

    @Test
    void testNoIntegerPartDecimal() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber(".89", "value", 2, result::set, false);
        assertTrue(isValid);
        assertEquals(".89", result.get());
    }

    @Test
    void validateDollarNumberReturnsFalseWhenNull() {
        AtomicReference<String> result = new AtomicReference<>();
        assertFalse(NumberUtil.validateDollarNumber(null, result::set));
    }

    @Test
    void validateDollarNumberReturnsFalseWhenEmpty() {
        AtomicReference<String> result = new AtomicReference<>();
        assertFalse(NumberUtil.validateDollarNumber("", result::set));
    }

    @Test
    void validateDollarNumberReturnsFalseWhenBothLeadingAndTrailingDollar() {
        AtomicReference<String> result = new AtomicReference<>();
        assertFalse(NumberUtil.validateDollarNumber("$10$", result::set));
    }

    @Test
    void validateDollarNumberReturnsFalseWhenNoDollarSign() {
        AtomicReference<String> result = new AtomicReference<>();
        assertFalse(NumberUtil.validateDollarNumber("10", result::set));
    }

    @Test
    void validateDollarNumberAcceptsLeadingDollar() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateDollarNumber("$100.50", result::set);
        assertTrue(isValid);
        assertEquals("100.50", result.get());
    }

    @Test
    void validateDollarNumberAcceptsTrailingDollar() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateDollarNumber("100.50$", result::set);
        assertTrue(isValid);
        assertEquals("100.50", result.get());
    }

    @Test
    void validateDollarNumberRejectsZeroAmount() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateDollarNumber("$0", result::set);
        assertFalse(isValid);
        assertNotNull(result.get());
        assertTrue(result.get().contains("zero") || result.get().contains("invalid"));
    }

    @Test
    void validateDollarNumberRejectsInvalidAmount() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateDollarNumber("$abc", result::set);
        assertFalse(isValid);
        assertTrue(result.get().contains("invalid"));
    }

    @Test
    void validateNumberWithIsDollarTrueRejectsAllKeyword() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("all", "test", 2, result::set, true);
        assertFalse(isValid);
        assertNotNull(result.get());
        assertTrue(result.get().contains("invalid"));
    }

    @Test
    void validateNumberAcceptsAllKeywordCaseInsensitiveWhenNotDollar() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("ALL", "test", 2, result::set, false);
        assertTrue(isValid);
        assertEquals("0", result.get());
    }

    @Test
    void validateNumberRejectsNegativeInput() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("-5", "quantity", 2, result::set, false);
        assertFalse(isValid);
        assertTrue(result.get().contains("invalid"));
    }

    @Test
    void validateNumberRejectsEmptyString() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateNumber("", "test", 2, result::set, false);
        assertFalse(isValid);
        assertTrue(result.get().contains("invalid"));
    }

    @Test
    void validateDollarNumberRejectsWhitespaceOnly() {
        AtomicReference<String> result = new AtomicReference<>();
        boolean isValid = NumberUtil.validateDollarNumber("$   ", result::set);
        assertFalse(isValid);
    }
}
