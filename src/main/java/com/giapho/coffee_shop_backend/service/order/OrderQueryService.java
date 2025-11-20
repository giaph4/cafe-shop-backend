package com.giapho.coffee_shop_backend.service.order;

import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.OrderSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface OrderQueryService {

    Page<OrderSummaryDTO> getOrderSummaries(Pageable pageable);

    Page<OrderSummaryDTO> getOrderSummariesByStatus(OrderStatus status, Pageable pageable);

    Page<OrderSummaryDTO> getOrderSummariesByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<OrderResponseDTO> getOrders(Pageable pageable);

    Page<OrderResponseDTO> getOrdersByStatus(String status, Pageable pageable);

    Page<OrderResponseDTO> getOrdersByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable);

    OrderResponseDTO getOrderDetail(Long orderId);

    OrderResponseDTO getPendingOrderByTable(Long tableId);
}
