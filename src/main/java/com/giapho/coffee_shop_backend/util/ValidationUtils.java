package com.giapho.coffee_shop_backend.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for common validation operations.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{10,15}$");

    /**
     * Validates if a string is a valid email address.
     *
     * @param email The email to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validates if a string is a valid phone number.
     *
     * @param phone The phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Checks if a string is null or empty after trimming.
     *
     * @param value The string to check
     * @return true if blank, false otherwise
     */
    public static boolean isBlank(String value) {
        return !StringUtils.hasText(value);
    }

    /**
     * Checks if a string has text after trimming.
     *
     * @param value The string to check
     * @return true if has text, false otherwise
     */
    public static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private ValidationUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
