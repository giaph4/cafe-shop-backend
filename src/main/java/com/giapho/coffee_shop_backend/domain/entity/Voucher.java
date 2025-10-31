package com.giapho.coffee_shop_backend.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Data
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // Mã voucher (VD: "GIAM10K", "FREESHIP")

    @Column(nullable = false)
    private String description; // Mô tả (VD: "Giảm 10,000 VND", "Miễn phí vận chuyển")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoucherType type; // Loại voucher (VD: FIXED_AMOUNT, PERCENTAGE)

    @Column(nullable = false)
    private BigDecimal discountValue; // Giá trị giảm (Số tiền cố định hoặc phần trăm)

    private BigDecimal minimumOrderAmount; // Số tiền đơn hàng tối thiểu để áp dụng

    private BigDecimal maximumDiscountAmount; // Số tiền giảm tối đa (cho loại PERCENTAGE)

    @Column(nullable = false)
    private LocalDateTime validFrom; // Ngày bắt đầu hiệu lực

    @Column(nullable = false)
    private LocalDateTime validTo; // Ngày hết hiệu lực

    @Column(nullable = false)
    private int usageLimit; // Số lần sử dụng tối đa

    @Column(nullable = false)
    private int timesUsed; // Số lần đã sử dụng

    @Column(nullable = false)
    private boolean active = true; // Trạng thái kích hoạt

    // Timestamps
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    public enum VoucherType {
        FIXED_AMOUNT, // Giảm số tiền cố định
        PERCENTAGE    // Giảm theo phần trăm
    }

    // Helper method để kiểm tra voucher có hợp lệ không (cơ bản)
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active && timesUsed < usageLimit && now.isAfter(validFrom) && now.isBefore(validTo);
    }
}