package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IngredientRequestDTO {

    @NotBlank(message = "Ingredient name is required")
    private String name;

    @NotBlank(message = "Unit is required (e.g., kg, g, ml, l, cai)")
    private String unit;

    @PositiveOrZero(message = "Reorder level must be zero or positive")
    private BigDecimal reorderLevel;
}