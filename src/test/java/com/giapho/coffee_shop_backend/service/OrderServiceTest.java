package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;
import com.giapho.coffee_shop_backend.mapper.OrderDetailMapper;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import com.giapho.coffee_shop_backend.service.impl.OrderServiceImpl;
import com.giapho.coffee_shop_backend.service.order.OrderPricingService;
import com.giapho.coffee_shop_backend.service.order.OrderQueryService;
import com.giapho.coffee_shop_backend.service.order.OrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CafeTableRepository cafeTableRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private OrderValidator orderValidator;
    @Mock
    private OrderPricingService orderPricingService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private OrderQueryService orderQueryService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUpMapperStub() {
        lenient().when(orderMapper.entityToResponse(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setId(order.getId());
            dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
            dto.setSubTotal(order.getSubTotal());
            dto.setDiscountAmount(order.getDiscountAmount());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setPaymentMethod(order.getPaymentMethod());
            dto.setVoucherCode(order.getVoucherCode());
            return dto;
        });
    }

    @Test
    void addItemToOrder_shouldAddNewDetailAndRecalculateTotals() {
        Long orderId = 1L;
        Long productId = 10L;

        Order order = buildPendingOrder(orderId);
        Product product = Product.builder()
                .id(productId)
                .name("Caramel Latte")
                .price(new BigDecimal("50000"))
                .isAvailable(true)
                .build();

        when(orderValidator.requirePendingOrder(orderId)).thenReturn(order);
        when(orderValidator.requireOrderWithDetails(orderId)).thenReturn(order);
        when(orderValidator.requireAvailableProduct(productId)).thenReturn(product);
        when(orderDetailMapper.requestToEntity(any(OrderDetailRequestDTO.class))).thenAnswer(invocation -> {
            OrderDetailRequestDTO dto = invocation.getArgument(0);
            return OrderDetail.builder()
                    .quantity(dto.getQuantity())
                    .notes(dto.getNotes())
                    .build();
        });

        OrderDetailRequestDTO request = new OrderDetailRequestDTO();
        request.setProductId(productId);
        request.setQuantity(2);
        request.setNotes("Less sugar");

        OrderResponseDTO response = orderService.addItemToOrder(orderId, request);

        assertThat(order.getOrderDetails()).hasSize(1);
        OrderDetail savedDetail = order.getOrderDetails().iterator().next();
        assertEquals(2, savedDetail.getQuantity());
        assertThat(savedDetail.getPriceAtOrder()).isEqualByComparingTo("50000");

        verify(orderPricingService).recalculateTotals(order);
        verify(orderRepository).save(order);
        verify(orderValidator).requireOrderWithDetails(orderId);

        assertThat(response.getSubTotal()).isEqualByComparingTo(order.getSubTotal());
        assertThat(response.getTotalAmount()).isEqualByComparingTo(order.getTotalAmount());
    }

    @Test
    void getOrderById_shouldDelegateToQueryService() {
        Long orderId = 100L;
        OrderResponseDTO expected = new OrderResponseDTO();
        expected.setId(orderId);

        when(orderQueryService.getOrderDetail(orderId)).thenReturn(expected);

        OrderResponseDTO result = orderService.getOrderById(orderId);

        assertThat(result).isSameAs(expected);
        verify(orderQueryService).getOrderDetail(orderId);
    }

    @Test
    void getPendingOrderByTable_shouldDelegateToQueryService() {
        Long tableId = 10L;
        OrderResponseDTO pendingOrder = new OrderResponseDTO();
        pendingOrder.setId(123L);

        when(orderQueryService.getPendingOrderByTable(tableId)).thenReturn(pendingOrder);

        OrderResponseDTO result = orderService.getPendingOrderByTable(tableId);

        assertThat(result).isSameAs(pendingOrder);
        verify(orderQueryService).getPendingOrderByTable(tableId);
    }

    @Test
    void applyVoucher_shouldUpdateDiscountAndTotalAmount() {
        Long orderId = 2L;
        Order order = buildPendingOrder(orderId);

        OrderDetail existingDetail = OrderDetail.builder()
                .order(order)
                .product(Product.builder().id(20L).price(new BigDecimal("75000")).build())
                .quantity(2)
                .priceAtOrder(new BigDecimal("75000"))
                .build();
        order.getOrderDetails().add(existingDetail);
        order.setSubTotal(new BigDecimal("150000"));
        order.setTotalAmount(new BigDecimal("150000"));

        when(orderValidator.requirePendingOrder(orderId)).thenReturn(order);
        when(orderValidator.requireOrderWithDetails(orderId)).thenReturn(order);
        doAnswer(invocation -> {
            Order target = invocation.getArgument(0);
            target.setVoucherCode("SAVE10");
            target.setDiscountAmount(new BigDecimal("15000"));
            target.setTotalAmount(new BigDecimal("135000"));
            return null;
        }).when(orderPricingService).applyVoucher(order, "SAVE10");

        OrderResponseDTO response = orderService.applyVoucher(orderId, "SAVE10");

        assertThat(order.getVoucherCode()).isEqualTo("SAVE10");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("15000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("135000");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("15000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("135000");

        verify(orderPricingService).applyVoucher(order, "SAVE10");
        verify(orderRepository).save(order);
    }

    @Test
    void payOrder_shouldDelegateToPaymentServiceAndUpdateTableStatus() {
        Long orderId = 3L;
        CafeTable table = CafeTable.builder().id(5L).name("T1").status(TableStatus.SERVING).build();
        Order paidOrder = buildPendingOrder(orderId);
        paidOrder.setStatus(OrderStatus.PAID);
        paidOrder.setCafeTable(table);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("cash");

        when(paymentService.processPayment(orderId, request)).thenReturn(paidOrder);
        when(orderValidator.requireOrderWithDetails(orderId)).thenReturn(paidOrder);
        when(orderRepository.findByTableIdAndStatus(table.getId(), OrderStatus.PENDING)).thenReturn(Optional.empty());

        OrderResponseDTO response = orderService.payOrder(orderId, request);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PAID.name());
        verify(paymentService).processPayment(orderId, request);
        verify(cafeTableRepository).save(table);
    }

    private Order buildPendingOrder(Long orderId) {
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .subTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .orderDetails(new HashSet<>())
                .build();
        if (order.getOrderDetails() == null) {
            order.setOrderDetails(new HashSet<>());
        }
        return order;
    }
}
