package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CafeTableRequest {

    @NotBlank(message = "Table name is required")
    private String name;

    @Min(value = 1, message = "Capacity must be least 1")
    private int capacity;
}
