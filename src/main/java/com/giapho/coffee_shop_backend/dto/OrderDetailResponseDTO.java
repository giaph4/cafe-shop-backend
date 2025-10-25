package com.giapho.coffee_shop_backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDetailResponseDTO {

    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal priceAtOrder;
    private String notes;
}
