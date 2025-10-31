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

        // Tính toán số tiền giảm giá
        BigDecimal discountAmount = calculateDiscount(voucher, orderAmount);

        return VoucherCheckResponseDTO.builder()
                .isValid(true)
                .message("Áp dụng voucher thành công!")
                .code(voucher.getCode())
                .discountAmount(discountAmount)
                .type(voucher.getType())
                .build();
    }

    // Hàm phụ tính toán discount
    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getType() == Voucher.VoucherType.FIXED_AMOUNT) {
            discount = voucher.getDiscountValue();
        } else if (voucher.getType() == Voucher.VoucherType.PERCENTAGE) {
            discount = orderAmount.multiply(voucher.getDiscountValue().divide(BigDecimal.valueOf(100)));
            // Áp dụng mức giảm tối đa nếu có
            if (voucher.getMaximumDiscountAmount() != null && discount.compareTo(voucher.getMaximumDiscountAmount()) > 0) {
                discount = voucher.getMaximumDiscountAmount();
            }
        }
        // Đảm bảo giảm giá không vượt quá tổng tiền
        return discount.min(orderAmount);
    }

    // Hàm phụ tạo response không hợp lệ
    private VoucherCheckResponseDTO buildInvalidResponse(Voucher voucher, String message) {
        return VoucherCheckResponseDTO.builder()
                .isValid(false)
                .message(message)
                .code(voucher.getCode())
                .discountAmount(BigDecimal.ZERO)
                .type(voucher.getType()) // Vẫn trả về type nếu muốn
                .build();
    }

    // Hàm này sẽ được gọi bởi OrderService khi đơn hàng hoàn thành
    public void incrementUsageCount(String code) {
        voucherRepository.findByCode(code).ifPresent(voucher -> {
            voucher.setTimesUsed(voucher.getTimesUsed() + 1);
            voucherRepository.save(voucher);
        });
    }

    // Thêm các hàm CRUD cơ bản (getAll, getById, create, update, delete) nếu cần quản lý voucher qua API
}