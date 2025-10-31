package com.giapho.coffee_shop_backend.dto;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class VoucherCheckResponseDTO {
    private boolean isValid;
    private String message; // Lý do không hợp lệ
    private String code;
    private BigDecimal discountAmount; // Số tiền giảm cụ thể cho đơn hàng này
    private Voucher.VoucherType type;
}