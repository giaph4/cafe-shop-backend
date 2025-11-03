package com.giapho.coffee_shop_backend.dto;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class VoucherCheckResponseDTO {
    private boolean isValid;
    private String message;
    private String code;
    private BigDecimal discountAmount;
    private Voucher.VoucherType type;
}