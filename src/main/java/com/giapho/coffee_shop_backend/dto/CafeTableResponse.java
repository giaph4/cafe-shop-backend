package com.giapho.coffee_shop_backend.dto;

import lombok.Data;

@Data
public class CafeTableResponse {

    private Long id;
    private String name;
    private int capacity;
    private String status;
}
