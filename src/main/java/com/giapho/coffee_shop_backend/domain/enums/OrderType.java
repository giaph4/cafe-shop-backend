package com.giapho.coffee_shop_backend.domain.enums;

/**
 * Hình thức phục vụ của đơn hàng.
 */
public enum OrderType {
    /**
     * Giá trị legacy tương đương với phục vụ tại bàn.
     */
    @Deprecated
    AT_TABLE,
    DINE_IN,
    TAKE_AWAY,
    DELIVERY,
    PICKUP,
    OTHER
}
