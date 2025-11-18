package com.giapho.coffee_shop_backend.service.voucher.helper;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class VoucherDiscountCalculator {

    public VoucherCheckResponseDTO evaluate(Voucher voucher, BigDecimal orderAmount) {
        if (!voucher.isActive()) {
            return buildInvalidResponse(voucher, "Voucher không hoạt động.");
        }
        if (voucher.getTimesUsed() >= voucher.getUsageLimit()) {
            return buildInvalidResponse(voucher, "Voucher đã hết lượt sử dụng.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getValidFrom())) {
            return buildInvalidResponse(voucher, "Voucher chưa đến ngày sử dụng.");
        }
        if (now.isAfter(voucher.getValidTo())) {
            return buildInvalidResponse(voucher, "Voucher đã hết hạn.");
        }
        if (voucher.getMinimumOrderAmount() != null && orderAmount.compareTo(voucher.getMinimumOrderAmount()) < 0) {
            return buildInvalidResponse(voucher,
                    "Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinimumOrderAmount() + ").");
        }

        BigDecimal discountAmount = calculateDiscount(voucher, orderAmount);
        return VoucherCheckResponseDTO.builder()
                .isValid(true)
                .message("Áp dụng voucher thành công!")
                .code(voucher.getCode())
                .discountAmount(discountAmount)
                .type(voucher.getType())
                .build();
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getType() == Voucher.VoucherType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        } else if (voucher.getType() == Voucher.VoucherType.PERCENTAGE) {
            discount = orderAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaximumDiscountAmount() != null
                    && discount.compareTo(voucher.getMaximumDiscountAmount()) > 0) {
                discount = voucher.getMaximumDiscountAmount();
            }
        }
        return discount.min(orderAmount);
    }

    public VoucherCheckResponseDTO buildNotFoundResponse(String code) {
        return VoucherCheckResponseDTO.builder()
                .isValid(false)
                .message("Voucher không tồn tại: " + code)
                .code(code)
                .discountAmount(BigDecimal.ZERO)
                .type(null)
                .build();
    }

    private VoucherCheckResponseDTO buildInvalidResponse(Voucher voucher, String message) {
        return VoucherCheckResponseDTO.builder()
                .isValid(false)
                .message(message)
                .code(voucher.getCode())
                .discountAmount(BigDecimal.ZERO)
                .type(voucher.getType())
                .build();
    }
}
