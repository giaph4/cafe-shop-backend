package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductIngredientDTO {

    private Long id; // Chỉ dùng khi hiển thị

    @NotNull(message = "Ingredient ID is required")
    private Long ingredientId;

    private String ingredientName; // Chỉ dùng khi hiển thị
    private String ingredientUnit; // Chỉ dùng khi hiển thị

    @NotNull(message = "Quantity needed is required")
    @Positive(message = "Quantity needed must be positive")
    private BigDecimal quantityNeeded;
}