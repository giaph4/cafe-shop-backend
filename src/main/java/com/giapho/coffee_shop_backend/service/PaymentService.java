package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;

/**
 * Định nghĩa hợp đồng xử lý thanh toán cho đơn hàng.
 */
public interface PaymentService {

    /**
     * Thực hiện thanh toán cho đơn hàng và cập nhật các thông tin liên quan.
     *
     * @param orderId        mã đơn hàng cần thanh toán
     * @param paymentRequest thông tin thanh toán (phương thức, khách hàng, voucher)
     * @return đơn hàng sau khi cập nhật trạng thái thanh toán
     */
    Order processPayment(Long orderId, PaymentRequestDTO paymentRequest);
}
