package com.giapho.coffee_shop_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class VoucherResponseDTO {
    private Long id;
    private String code;
    private String description;
    private Voucher.VoucherType type;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscountAmount;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validFrom;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validTo;
    private Integer usageLimit;
    private Integer timesUsed;
    private Boolean active;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
