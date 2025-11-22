package com.giapho.coffee_shop_backend.util.constants;

/**
 * Constants for payment method values.
 */
public final class PaymentMethodConstants {

    /**
     * Cash payment
     */
    public static final String CASH = "CASH";

    /**
     * Credit/Debit card payment
     */
    public static final String CARD = "CARD";

    /**
     * Bank transfer
     */
    public static final String TRANSFER = "TRANSFER";

    /**
     * E-wallet payment (Momo, ZaloPay, etc.)
     */
    public static final String E_WALLET = "E_WALLET";

    /**
     * Private constructor to prevent instantiation
     */
    private PaymentMethodConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
