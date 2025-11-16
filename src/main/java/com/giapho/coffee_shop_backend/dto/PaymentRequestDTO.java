package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequestDTO {

    @NotBlank(message = "Payment method is required (e.g., CASH, TRANSFER, CARD)")
    private String paymentMethod; // "CASH", "TRANSFER", "CARD"

    private Long customerId; // Optional: to associate customer during payment

    private String voucherCode; // Optional: apply voucher during payment
}
