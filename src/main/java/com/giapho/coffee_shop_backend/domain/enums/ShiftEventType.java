package com.giapho.coffee_shop_backend.domain.enums;

/**
 * Các loại sự kiện ca làm việc dùng cho log và realtime notification.
 */
public enum ShiftEventType {
    SESSION_STARTED,
    SESSION_ENDED,
    SESSION_FORCED,
    ORDER_TRANSFERRED
}
