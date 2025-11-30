package com.giapho.coffee_shop_backend.domain.enums;

/**
 * Trạng thái thanh toán
 */
public enum PaymentStatus {
    /**
     * Chờ thanh toán
     */
    PENDING,
    
    /**
     * Đã thanh toán một phần
     */
    PARTIALLY_PAID,
    
    /**
     * Đã thanh toán đầy đủ
     */
    PAID,
    
    /**
     * Đã hoàn tiền một phần
     */
    PARTIALLY_REFUNDED,
    
    /**
     * Đã hoàn tiền toàn bộ
     */
    REFUNDED,
    
    /**
     * Thanh toán thất bại
     */
    FAILED,
    
    /**
     * Đã hủy thanh toán
     */
    CANCELLED,
    
    /**
     * Đang xử lý
     */
    PROCESSING,
    
    /**
     * Đã quá hạn thanh toán
     */
    EXPIRED
}
