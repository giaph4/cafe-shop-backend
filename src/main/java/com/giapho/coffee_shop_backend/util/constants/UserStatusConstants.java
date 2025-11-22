package com.giapho.coffee_shop_backend.util.constants;

/**
 * Constants for user status values. Used throughout the application to maintain
 * consistency.
 */
public final class UserStatusConstants {

    /**
     * User account is active and can log in
     */
    public static final String ACTIVE = "ACTIVE";

    /**
     * User account is inactive/disabled
     */
    public static final String INACTIVE = "INACTIVE";

    /**
     * User account is suspended
     */
    public static final String SUSPENDED = "SUSPENDED";

    /**
     * Private constructor to prevent instantiation
     */
    private UserStatusConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
