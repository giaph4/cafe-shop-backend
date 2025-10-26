package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseOrderRequestDTO {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private LocalDateTime expectedDate; // Ngày dự kiến nhận (tùy chọn)

    @NotEmpty(message = "Purchase order must contain at least one item")
    @Valid // Validate các item bên trong list
    private List<PurchaseOrderDetailRequestDTO> items;
}