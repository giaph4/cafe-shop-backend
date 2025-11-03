package com.giapho.coffee_shop_backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PurchaseOrderResponseDTO {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private String staffUsername;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDate;
    private String status;
    private BigDecimal totalAmount;
    private Set<PurchaseOrderDetailResponseDTO> purchaseOrderDetails;
}