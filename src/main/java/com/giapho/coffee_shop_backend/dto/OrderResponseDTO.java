package com.giapho.coffee_shop_backend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class OrderResponseDTO {

    private Long id;
    private String tableName;
    private String staffUsername;
    private String type;
    private String status;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String paymentMethod;
    private Set<OrderDetailResponseDTO> orderDetails;
}