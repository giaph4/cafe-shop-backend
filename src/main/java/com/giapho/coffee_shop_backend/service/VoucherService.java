package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.domain.repository.VoucherRepository;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    // Inject VoucherMapper nếu cần cho CRUD

    // Hàm quan trọng để kiểm tra và tính toán giảm giá
    public VoucherCheckResponseDTO checkAndCalculateDiscount(String code, BigDecimal orderAmount) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Voucher không tồn tại: " + code));

        // Kiểm tra cơ bản
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
            return buildInvalidResponse(voucher, "Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinimumOrderAmount() + ").");
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
            discount = orderAmount.multiply(voucher.getDiscountValue().divide(BigDecimal.valueOf(100)));

            if (voucher.getMaximumDiscountAmount() != null && discount.compareTo(voucher.getMaximumDiscountAmount()) > 0) {
                discount = voucher.getMaximumDiscountAmount();
            }
        }

        return discount.min(orderAmount);
    }


    private VoucherCheckResponseDTO buildInvalidResponse(Voucher voucher, String message) {
        return VoucherCheckResponseDTO.builder()
                .isValid(false)
                .message(message)
                .code(voucher.getCode())
                .discountAmount(BigDecimal.ZERO)
                .type(voucher.getType()) // Vẫn trả về type nếu muốn
                .build();
    }


    public void incrementUsageCount(String code) {
        voucherRepository.findByCode(code).ifPresent(voucher -> {
            voucher.setTimesUsed(voucher.getTimesUsed() + 1);
            voucherRepository.save(voucher);
        });
    }
}