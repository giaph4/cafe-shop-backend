package com.giapho.coffee_shop_backend.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CategoryResponse {

    private final Long id;
    private final String name;
    private final String description;
}
