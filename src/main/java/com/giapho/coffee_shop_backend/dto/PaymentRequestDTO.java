package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Đối tượng DTO chứa thông tin thanh toán đơn hàng
 */
@Data
public class PaymentRequestDTO {

    /**
     * Phương thức thanh toán (CASH, TRANSFER, CARD)
     */
    @NotBlank(message = "Phương thức thanh toán là bắt buộc (ví dụ: CASH, TRANSFER, CARD)")
    private String paymentMethod;

    /**
     * ID khách hàng (không bắt buộc)
     */
    private Long customerId;

    /**
     * Mã voucher áp dụng (không bắt buộc)
     */
    private String voucherCode;

    /**
     * Số tiền tip (không bắt buộc, mặc định là 0)
     */
    @DecimalMin(value = "0.0", message = "Số tiền tip không được nhỏ hơn 0")
    @NotNull(message = "Số tiền tip không được để trống")
    private BigDecimal tipAmount = BigDecimal.ZERO;
}
