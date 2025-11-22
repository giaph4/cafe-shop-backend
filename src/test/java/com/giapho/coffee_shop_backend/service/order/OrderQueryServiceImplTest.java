package com.giapho.coffee_shop_backend.service.order;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.OrderType;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.projection.OrderSummaryProjection;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.OrderSummaryDTO;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderValidator orderValidator;

    @InjectMocks
    private OrderQueryServiceImpl orderQueryService;

    private PageRequest pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getOrderSummaries_ShouldMapProjectionToDto() {
        OrderSummaryProjection projection = mock(OrderSummaryProjection.class);
        Page<OrderSummaryProjection> projectionPage = new PageImpl<>(List.of(projection));
        OrderSummaryDTO summary = new OrderSummaryDTO(1L, "T1", "staff01", OrderType.DINE_IN, OrderStatus.PAID,
                BigDecimal.ONE, BigDecimal.TEN, LocalDateTime.now(), LocalDateTime.now());

        when(orderRepository.findOrderSummaries(pageable)).thenReturn(projectionPage);
        when(orderMapper.toSummary(projection)).thenReturn(summary);

        Page<OrderSummaryDTO> result = orderQueryService.getOrderSummaries(pageable);

        assertThat(result.getContent()).containsExactly(summary);
        verify(orderMapper).toSummary(projection);
    }

    @Test
    void getOrderSummariesByStatus_ShouldDelegateToRepository() {
        OrderSummaryProjection projection = mock(OrderSummaryProjection.class);
        Page<OrderSummaryProjection> projectionPage = new PageImpl<>(List.of(projection));
        OrderSummaryDTO summary = new OrderSummaryDTO(2L, "T2", "staff02", OrderType.DELIVERY, OrderStatus.CANCELLED,
                BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(), null);

        when(orderRepository.findOrderSummariesByStatus(OrderStatus.CANCELLED, pageable)).thenReturn(projectionPage);
        when(orderMapper.toSummary(projection)).thenReturn(summary);

        Page<OrderSummaryDTO> result = orderQueryService.getOrderSummariesByStatus(OrderStatus.CANCELLED, pageable);

        assertThat(result.getContent()).containsExactly(summary);
        verify(orderRepository).findOrderSummariesByStatus(OrderStatus.CANCELLED, pageable);
    }

    @Test
    void getOrderSummariesByDateRange_ShouldConvertDatesToDateTime() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        OrderSummaryProjection projection = mock(OrderSummaryProjection.class);
        Page<OrderSummaryProjection> projectionPage = new PageImpl<>(List.of(projection));
        OrderSummaryDTO summary = new OrderSummaryDTO(3L, "T3", "staff03", OrderType.TAKE_AWAY, OrderStatus.PAID,
                BigDecimal.ONE, BigDecimal.TWO, LocalDateTime.now(), null);

        when(orderRepository.findOrderSummariesByCreatedAtBetween(any(), any(), eq(pageable))).thenReturn(projectionPage);
        when(orderMapper.toSummary(projection)).thenReturn(summary);

        Page<OrderSummaryDTO> result = orderQueryService.getOrderSummariesByDateRange(startDate, endDate, pageable);

        assertThat(result.getContent()).containsExactly(summary);
        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderRepository).findOrderSummariesByCreatedAtBetween(startCaptor.capture(), endCaptor.capture(), eq(pageable));
        assertThat(startCaptor.getValue()).isEqualTo(startDate.atStartOfDay());
        assertThat(endCaptor.getValue()).isEqualTo(endDate.plusDays(1).atStartOfDay());
    }

    @Test
    void getOrderDetail_ShouldReturnMappedDto() {
        Long orderId = 10L;
        Order order = mock(Order.class);
        OrderResponseDTO responseDTO = mock(OrderResponseDTO.class);

        when(orderValidator.requireOrderWithDetails(orderId)).thenReturn(order);
        when(orderMapper.entityToResponse(order)).thenReturn(responseDTO);

        OrderResponseDTO result = orderQueryService.getOrderDetail(orderId);

        assertThat(result).isSameAs(responseDTO);
        verify(orderValidator).requireOrderWithDetails(orderId);
        verify(orderMapper).entityToResponse(order);
    }
}
