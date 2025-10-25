package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OrderDetailUpdateRequestDTO {

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    private String notes;
}
