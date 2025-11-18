package com.giapho.coffee_shop_backend.service.voucher.helper;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class VoucherMapper {

    public Voucher toEntity(VoucherRequestDTO request) {
        Voucher voucher = new Voucher();
        updateEntity(request, voucher);
        return voucher;
    }

    public void updateEntity(VoucherRequestDTO request, Voucher voucher) {
        voucher.setDescription(trim(request.getDescription()));
        voucher.setType(request.getType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMinimumOrderAmount(request.getMinimumOrderAmount());
        voucher.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        voucher.setValidFrom(truncate(request.getValidFrom()));
        voucher.setValidTo(truncate(request.getValidTo()));
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
    }

    public VoucherResponseDTO toResponse(Voucher voucher) {
        return VoucherResponseDTO.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .description(voucher.getDescription())
                .type(voucher.getType())
                .discountValue(voucher.getDiscountValue())
                .minimumOrderAmount(voucher.getMinimumOrderAmount())
                .maximumDiscountAmount(voucher.getMaximumDiscountAmount())
                .validFrom(voucher.getValidFrom())
                .validTo(voucher.getValidTo())
                .usageLimit(voucher.getUsageLimit())
                .timesUsed(voucher.getTimesUsed())
                .active(voucher.isActive())
                .createdAt(voucher.getCreatedAt())
                .updatedAt(voucher.getUpdatedAt())
                .build();
    }

    private LocalDateTime truncate(LocalDateTime value) {
        return value != null ? value.truncatedTo(ChronoUnit.SECONDS) : null;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
