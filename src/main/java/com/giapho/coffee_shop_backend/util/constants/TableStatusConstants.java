package com.giapho.coffee_shop_backend.util.constants;

/**
 * Constants for cafe table status values.
 */
public final class TableStatusConstants {

    /**
     * Table is empty and available
     */
    public static final String EMPTY = "EMPTY";

    /**
     * Table is currently serving customers
     */
    public static final String SERVING = "SERVING";

    /**
     * Table is reserved
     */
    public static final String RESERVED = "RESERVED";

    /**
     * Table is under maintenance
     */
    public static final String MAINTENANCE = "MAINTENANCE";

    /**
     * Private constructor to prevent instantiation
     */
    private TableStatusConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
