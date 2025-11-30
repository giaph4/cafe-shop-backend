package com.giapho.coffee_shop_backend.service.order;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
import com.giapho.coffee_shop_backend.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final VoucherService voucherService;

    /**
     * Tính toán lại tổng tiền đơn hàng, bao gồm cả tiền tip
     * @param order Đơn hàng cần tính toán lại
     */
    public void recalculateTotals(Order order) {
        // Tính tổng tiền trước giảm giá
        BigDecimal subTotal = calculateSubTotal(order);
        order.setSubTotal(subTotal);

        // Nếu không có voucher, đặt lại giảm giá về 0 và tính tổng tiền cuối cùng
        if (!hasVoucher(order)) {
            resetDiscount(order);
            // Cộng thêm tiền tip vào tổng tiền cuối cùng
            order.setTotalAmount(calculateFinalTotal(order, subTotal, order.getDiscountAmount()));
            return;
        }

        // Kiểm tra và tính toán giảm giá từ voucher
        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(order.getVoucherCode(), subTotal);
        if (!voucherCheck.isValid()) {
            log.warn("Voucher {} invalid for order {}: {}", order.getVoucherCode(), order.getId(), voucherCheck.getMessage());
            resetVoucher(order);
            // Cộng thêm tiền tip vào tổng tiền cuối cùng
            order.setTotalAmount(calculateFinalTotal(order, subTotal, order.getDiscountAmount()));
            return;
        }

        // Áp dụng giảm giá
        BigDecimal discount = defaultZero(voucherCheck.getDiscountAmount()).min(subTotal);
        order.setDiscountAmount(discount);
        
        // Tính tổng tiền cuối cùng sau khi đã trừ giảm giá và cộng tiền tip
        order.setTotalAmount(calculateFinalTotal(order, subTotal, discount));
    }

    /**
     * Áp dụng voucher cho đơn hàng
     * @param order Đơn hàng cần áp dụng voucher
     * @param voucherCode Mã voucher
     * @throws VoucherInvalidException Nếu voucher không hợp lệ
     */
    public void applyVoucher(Order order, String voucherCode) {
        if (!hasItems(order)) {
            throw new VoucherInvalidException("Không thể áp dụng voucher cho đơn hàng trống");
        }

        String normalizedCode = voucherCode.trim().toUpperCase();
        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(normalizedCode, order.getSubTotal());
        if (!voucherCheck.isValid()) {
            throw new VoucherInvalidException(normalizedCode, voucherCheck.getMessage());
        }

        BigDecimal discount = defaultZero(voucherCheck.getDiscountAmount()).min(order.getSubTotal());
        order.setVoucherCode(normalizedCode);
        order.setDiscountAmount(discount);
        
        // Cập nhật tổng tiền cuối cùng sau khi áp dụng giảm giá và cộng tiền tip
        order.setTotalAmount(calculateFinalTotal(order, order.getSubTotal(), discount));
        
        log.info("Áp dụng voucher {} cho đơn hàng {}. Giảm giá: {}, Tiền tip: {}", 
                normalizedCode, order.getId(), discount, order.getTipAmount());
    }

    public void removeVoucher(Order order) {
        resetVoucher(order);
    }

    private BigDecimal calculateSubTotal(Order order) {
        if (CollectionUtils.isEmpty(order.getOrderDetails())) {
            return BigDecimal.ZERO;
        }
        return order.getOrderDetails().stream()
                .map(this::calculateLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateLineAmount(OrderDetail detail) {
        if (detail.getPriceAtOrder() == null || detail.getQuantity() <= 0) {
            return BigDecimal.ZERO;
        }
        return detail.getPriceAtOrder().multiply(BigDecimal.valueOf(detail.getQuantity()));
    }

    private void resetVoucher(Order order) {
        order.setVoucherCode(null);
        resetDiscount(order);
    }

    /**
     * Tính tổng tiền cuối cùng sau khi đã trừ giảm giá và cộng tiền tip
     * @param order Đơn hàng
     * @param subTotal Tổng tiền trước giảm giá
     * @param discount Số tiền được giảm
     * @return Tổng tiền cuối cùng
     */
    private BigDecimal calculateFinalTotal(Order order, BigDecimal subTotal, BigDecimal discount) {
        BigDecimal tipAmount = defaultZero(order.getTipAmount());
        return subTotal.subtract(discount).add(tipAmount);
    }

    /**
     * Đặt lại giảm giá về 0
     * @param order Đơn hàng cần đặt lại
     */
    private void resetDiscount(Order order) {
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(calculateFinalTotal(order, order.getSubTotal(), BigDecimal.ZERO));
    }

    private boolean hasVoucher(Order order) {
        return order != null && StringUtils.hasText(order.getVoucherCode());
    }

    private boolean hasItems(Order order) {
        return order.getOrderDetails() != null && !order.getOrderDetails().isEmpty();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
