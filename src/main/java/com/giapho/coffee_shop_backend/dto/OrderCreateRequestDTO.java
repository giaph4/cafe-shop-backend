package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequestDTO {

    private Long tableId;

    @NotBlank(message = "Order type is required")
    private String type;

    private Long customerId;

    private String voucherCode;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderDetailRequestDTO> items;
}
