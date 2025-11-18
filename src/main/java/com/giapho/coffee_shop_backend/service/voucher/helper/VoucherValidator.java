package com.giapho.coffee_shop_backend.service.voucher.helper;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherRequestDTO;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherValidationException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherConflictException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Predicate;

@Component
public class VoucherValidator {

    public void validateBusinessRules(VoucherRequestDTO request) {
        if (!StringUtils.hasText(request.getCode())) {
            throw new VoucherValidationException("Mã voucher không được để trống");
        }
        if (request.getValidFrom().isAfter(request.getValidTo())) {
            throw new VoucherValidationException("Ngày bắt đầu phải trước ngày kết thúc");
        }
        if (request.getType() == Voucher.VoucherType.PERCENTAGE
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new VoucherValidationException("Voucher phần trăm không được lớn hơn 100");
        }
        if (request.getMaximumDiscountAmount() != null
                && request.getType() == Voucher.VoucherType.FIXED_AMOUNT
                && request.getMaximumDiscountAmount().compareTo(request.getDiscountValue()) < 0) {
            throw new VoucherValidationException("Giá trị giảm tối đa không được nhỏ hơn giá trị giảm cố định");
        }
    }

    public void ensureUsageLimitNotLessThanTimesUsed(Voucher voucher, Integer usageLimit) {
        if (usageLimit != null && voucher.getTimesUsed() > usageLimit) {
            throw new VoucherValidationException("usageLimit không thể nhỏ hơn số lượt đã sử dụng");
        }
    }

    public void ensureCodeUnique(String normalizedCode, Predicate<String> existsByCode) {
        if (existsByCode.test(normalizedCode)) {
            throw new VoucherConflictException("Voucher code đã tồn tại: " + normalizedCode);
        }
    }

    public void ensureCodeAvailableForUpdate(Voucher existing, String normalizedCode, Predicate<String> existsByCode) {
        if (!existing.getCode().equalsIgnoreCase(normalizedCode) && existsByCode.test(normalizedCode)) {
            throw new VoucherConflictException("Voucher code đã tồn tại: " + normalizedCode);
        }
    }

    public void validateDiscountRequest(String code, BigDecimal orderAmount) {
        if (!StringUtils.hasText(code)) {
            throw new VoucherValidationException("Voucher code không được bỏ trống");
        }
        if (orderAmount == null || orderAmount.signum() < 0) {
            throw new VoucherValidationException("Giá trị đơn hàng phải lớn hơn hoặc bằng 0");
        }
    }

    public String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new VoucherValidationException("Voucher code không được bỏ trống");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
