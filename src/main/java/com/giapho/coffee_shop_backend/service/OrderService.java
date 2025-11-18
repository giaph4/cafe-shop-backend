package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.OrderCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderService {

    Page<OrderResponseDTO> getAllOrders(Pageable pageable);

    Page<OrderResponseDTO> getOrdersByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    OrderResponseDTO getOrderById(Long id);

    OrderResponseDTO getPendingOrderByTable(Long tableId);

    OrderResponseDTO createOrder(OrderCreateRequestDTO request);

    OrderResponseDTO addItemToOrder(Long orderId, OrderDetailRequestDTO itemDTO);

    OrderResponseDTO updateItemInOrder(Long orderId, Long orderDetailId, OrderDetailUpdateRequestDTO updateDTO);

    OrderResponseDTO removeItemFromOrder(Long orderId, Long orderDetailId);

    OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest);

    OrderResponseDTO applyVoucher(Long orderId, String voucherCode);

    OrderResponseDTO removeVoucher(Long orderId);

    Page<OrderResponseDTO> getOrdersByStatus(String status, Pageable pageable);

    OrderResponseDTO cancelOrder(Long orderId);
}