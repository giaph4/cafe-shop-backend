package com.giapho.coffee_shop_backend.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for date and time operations.
 */
public final class DateTimeUtils {

    /**
     * Gets the start of day (00:00:00) for a given date.
     *
     * @param date The date
     * @return LocalDateTime at start of day
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * Gets the end of day (23:59:59.999999999) for a given date.
     *
     * @param date The date
     * @return LocalDateTime at end of day
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * Checks if a date is between two dates (inclusive).
     *
     * @param date The date to check
     * @param start The start date
     * @param end The end date
     * @return true if between, false otherwise
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Checks if a datetime is between two datetimes (inclusive).
     *
     * @param dateTime The datetime to check
     * @param start The start datetime
     * @param end The end datetime
     * @return true if between, false otherwise
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * Calculates the number of days between two dates.
     *
     * @param start The start date
     * @param end The end date
     * @return Number of days
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Checks if a date is today.
     *
     * @param date The date to check
     * @return true if today, false otherwise
     */
    public static boolean isToday(LocalDate date) {
        return date.equals(LocalDate.now());
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private DateTimeUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
