package com.giapho.coffee_shop_backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String code;
    private BigDecimal price;
    private BigDecimal cost;
    private String description;
    private String imageUrl;
    private boolean available;
    private LocalDateTime createdAt;
    private String categoryName;
}
