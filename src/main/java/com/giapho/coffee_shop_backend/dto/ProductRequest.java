package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product code is required")
    private String code;

    @NotNull(message = "Product price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private BigDecimal cost;

    private String description;

    private String imageUrl;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
