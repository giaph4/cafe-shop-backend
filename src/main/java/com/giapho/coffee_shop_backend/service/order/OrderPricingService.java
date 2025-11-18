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

    public void recalculateTotals(Order order) {
        BigDecimal subTotal = calculateSubTotal(order);
        order.setSubTotal(subTotal);

        if (!hasVoucher(order)) {
            resetDiscount(order);
            return;
        }

        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(order.getVoucherCode(), subTotal);
        if (!voucherCheck.isValid()) {
            log.warn("Voucher {} invalid for order {}: {}", order.getVoucherCode(), order.getId(), voucherCheck.getMessage());
            resetVoucher(order);
            return;
        }

        BigDecimal discount = defaultZero(voucherCheck.getDiscountAmount()).min(subTotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(subTotal.subtract(discount));
    }

    public void applyVoucher(Order order, String voucherCode) {
        if (!hasItems(order)) {
            throw new VoucherInvalidException("Order has no items to apply voucher");
        }

        String normalizedCode = voucherCode.trim().toUpperCase();
        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(normalizedCode, order.getSubTotal());
        if (!voucherCheck.isValid()) {
            throw new VoucherInvalidException(normalizedCode, voucherCheck.getMessage());
        }

        BigDecimal discount = defaultZero(voucherCheck.getDiscountAmount()).min(order.getSubTotal());
        order.setVoucherCode(normalizedCode);
        order.setDiscountAmount(discount);
        order.setTotalAmount(order.getSubTotal().subtract(discount));
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

    private void resetDiscount(Order order) {
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(order.getSubTotal());
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
