package com.giapho.coffee_shop_backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PurchaseOrderDetailResponseDTO {
    private Long id;
    private Long ingredientId;
    private String ingredientName; // Tên nguyên liệu
    private String ingredientUnit; // Đơn vị tính
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal; // Thành tiền = quantity * unitPrice
}