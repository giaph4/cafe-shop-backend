package com.giapho.coffee_shop_backend.service.order;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.OrderSummaryDTO;
import com.giapho.coffee_shop_backend.exception.order.OrderNotFoundException;
import com.giapho.coffee_shop_backend.exception.order.TableNotFoundException;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final CafeTableRepository cafeTableRepository;
    private final OrderMapper orderMapper;
    private final OrderValidator orderValidator;

    @Override
    public Page<OrderSummaryDTO> getOrderSummaries(Pageable pageable) {
        return orderRepository.findOrderSummaries(pageable)
                .map(orderMapper::toSummary);
    }

    @Override
    public Page<OrderSummaryDTO> getOrderSummariesByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findOrderSummariesByStatus(status, pageable)
                .map(orderMapper::toSummary);
    }

    @Override
    public Page<OrderSummaryDTO> getOrderSummariesByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return orderRepository.findOrderSummariesByCreatedAtBetween(start, end, pageable)
                .map(orderMapper::toSummary);
    }

    @Override
    public Page<OrderResponseDTO> getOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(orderMapper::entityToResponse);
    }

    @Override
    public Page<OrderResponseDTO> getOrdersByStatus(String status, Pageable pageable) {
        if (!StringUtils.hasText(status)) {
            return getOrders(pageable);
        }
        OrderStatus parsedStatus = orderValidator.parseOrderStatus(status);
        return orderRepository.findByStatus(parsedStatus, pageable)
                .map(orderMapper::entityToResponse);
    }

    @Override
    public Page<OrderResponseDTO> getOrdersByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return orderRepository.findByCreatedAtBetween(start, end, pageable)
                .map(orderMapper::entityToResponse);
    }

    @Override
    public OrderResponseDTO getOrderDetail(Long orderId) {
        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    @Override
    public OrderResponseDTO getPendingOrderByTable(Long tableId) {
        if (!cafeTableRepository.existsById(tableId)) {
            throw new TableNotFoundException(tableId);
        }
        Order order = orderRepository.findByTableIdAndStatus(tableId, OrderStatus.PENDING)
                .orElseThrow(() -> new OrderNotFoundException("No pending order found for table id: " + tableId));
        return orderMapper.entityToResponse(order);
    }
}
