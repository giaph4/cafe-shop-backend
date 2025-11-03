package com.giapho.coffee_shop_backend.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class OrderResponseDTO {

    private Long id;
    private String tableName;
    private String staffUsername;
    private String type;
    private String status;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String voucherCode;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private Set<OrderDetailResponseDTO> orderDetails;
}