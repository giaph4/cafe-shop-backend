package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPurchaseHistoryResponseDTO {

    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long totalOrders;
    private BigDecimal totalAmount;
    private BigDecimal averageOrderValue;
    private List<CustomerPurchaseHistoryItemDTO> orders;
    private LocalDateTime lastPurchaseDate;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
