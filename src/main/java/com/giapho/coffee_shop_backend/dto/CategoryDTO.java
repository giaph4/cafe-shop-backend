package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryDTO {
    private Long id;

    private String name;

    private String description;
}
