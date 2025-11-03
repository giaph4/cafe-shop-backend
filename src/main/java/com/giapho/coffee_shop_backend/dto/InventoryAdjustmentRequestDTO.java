package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class InventoryAdjustmentRequestDTO {

    @NotNull(message = "Ingredient ID is required")
    private Long ingredientId;

    @NotNull(message = "New quantity on hand is required")
    @PositiveOrZero(message = "Quantity must be zero or positive")
    private BigDecimal newQuantityOnHand;

    private String reason;
}