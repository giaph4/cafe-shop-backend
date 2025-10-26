package com.giapho.coffee_shop_backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IngredientResponseDTO {

    private Long id;
    private String name;
    private String unit;
    private BigDecimal quantityOnHand; // Hiển thị tồn kho hiện tại
    private BigDecimal reorderLevel;
}