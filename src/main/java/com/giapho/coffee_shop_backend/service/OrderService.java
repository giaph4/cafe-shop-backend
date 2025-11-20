package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.OrderCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;

public interface OrderService {

    OrderResponseDTO getOrderById(Long id);

    OrderResponseDTO getPendingOrderByTable(Long tableId);

    OrderResponseDTO createOrder(OrderCreateRequestDTO request);

    OrderResponseDTO addItemToOrder(Long orderId, OrderDetailRequestDTO itemDTO);

    OrderResponseDTO updateItemInOrder(Long orderId, Long orderDetailId, OrderDetailUpdateRequestDTO updateDTO);

    OrderResponseDTO removeItemFromOrder(Long orderId, Long orderDetailId);

    OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest);

    OrderResponseDTO applyVoucher(Long orderId, String voucherCode);

    OrderResponseDTO removeVoucher(Long orderId);

    OrderResponseDTO cancelOrder(Long orderId);
}