package com.giapho.coffee_shop_backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for money/currency calculations.
 */
public final class MoneyUtils {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Rounds a money value to 2 decimal places.
     *
     * @param amount The amount to round
     * @return Rounded amount
     */
    public static BigDecimal round(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Checks if an amount is positive.
     *
     * @param amount The amount to check
     * @return true if positive, false otherwise
     */
    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if an amount is zero or negative.
     *
     * @param amount The amount to check
     * @return true if zero or negative, false otherwise
     */
    public static boolean isZeroOrNegative(BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Calculates percentage of a total.
     *
     * @param part The part value
     * @param total The total value
     * @return Percentage (0-100)
     */
    public static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (isZeroOrNegative(total)) {
            return BigDecimal.ZERO;
        }
        return part.divide(total, SCALE + 2, ROUNDING_MODE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Applies a percentage discount to an amount.
     *
     * @param amount The original amount
     * @param discountPercent The discount percentage (0-100)
     * @return Amount after discount
     */
    public static BigDecimal applyDiscount(BigDecimal amount, BigDecimal discountPercent) {
        if (amount == null || discountPercent == null) {
            return amount;
        }
        BigDecimal discountAmount = amount.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        return round(amount.subtract(discountAmount));
    }

    /**
     * Safely adds two money values.
     *
     * @param a First value
     * @param b Second value
     * @return Sum
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        BigDecimal valueA = a != null ? a : BigDecimal.ZERO;
        BigDecimal valueB = b != null ? b : BigDecimal.ZERO;
        return round(valueA.add(valueB));
    }

    /**
     * Safely subtracts two money values.
     *
     * @param a First value
     * @param b Second value
     * @return Difference
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal valueA = a != null ? a : BigDecimal.ZERO;
        BigDecimal valueB = b != null ? b : BigDecimal.ZERO;
        return round(valueA.subtract(valueB));
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private MoneyUtils() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
