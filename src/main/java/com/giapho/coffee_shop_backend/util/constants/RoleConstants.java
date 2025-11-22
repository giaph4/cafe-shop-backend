package com.giapho.coffee_shop_backend.util.constants;

/**
 * Constants for user role names. Used in @PreAuthorize annotations and role
 * checks.
 */
public final class RoleConstants {

    /**
     * Administrator role - full system access
     */
    public static final String ADMIN = "ADMIN";

    /**
     * Role name with ROLE_ prefix for Spring Security
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Manager role - business management access
     */
    public static final String MANAGER = "MANAGER";

    /**
     * Role name with ROLE_ prefix for Spring Security
     */
    public static final String ROLE_MANAGER = "ROLE_MANAGER";

    /**
     * Staff role - operational access
     */
    public static final String STAFF = "STAFF";

    /**
     * Role name with ROLE_ prefix for Spring Security
     */
    public static final String ROLE_STAFF = "ROLE_STAFF";

    /**
     * Private constructor to prevent instantiation
     */
    private RoleConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
