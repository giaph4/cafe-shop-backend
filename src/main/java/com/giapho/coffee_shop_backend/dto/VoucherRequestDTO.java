package com.giapho.coffee_shop_backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequestDTO {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    private Voucher.VoucherType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal minimumOrderAmount;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal maximumDiscountAmount;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validFrom;

    @NotNull
    @Future
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime validTo;

    @NotNull
    @Positive
    private Integer usageLimit;

    private Boolean active;
}
