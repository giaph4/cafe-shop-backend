package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CafeTableRepository cafeTableRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private VoucherService voucherService;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUpMapperStub() {
        lenient().when(orderMapper.entityToResponse(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            OrderResponseDTO dto = new OrderResponseDTO();
            dto.setId(order.getId());
            dto.setStatus(order.getStatus());
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

        when(orderRepository.findPendingOrderByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDetailRequestDTO request = new OrderDetailRequestDTO();
        request.setProductId(productId);
        request.setQuantity(2);
        request.setNotes("Less sugar");

        OrderResponseDTO response = orderService.addItemToOrder(orderId, request);

        assertThat(order.getOrderDetails()).hasSize(1);
        OrderDetail savedDetail = order.getOrderDetails().iterator().next();
        assertEquals(2, savedDetail.getQuantity());
        assertThat(savedDetail.getPriceAtOrder()).isEqualByComparingTo("50000");

        assertThat(order.getSubTotal()).isEqualByComparingTo("100000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("100000");
        assertThat(response.getSubTotal()).isEqualByComparingTo("100000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100000");

        verify(orderRepository).save(order);
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

        when(orderRepository.findPendingOrderByIdWithDetails(orderId)).thenReturn(Optional.of(order));
        when(voucherService.checkAndCalculateDiscount(eq("SAVE10"), eq(new BigDecimal("150000"))))
                .thenReturn(VoucherCheckResponseDTO.builder()
                        .isValid(true)
                        .discountAmount(new BigDecimal("15000"))
                        .message("OK")
                        .build());
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponseDTO response = orderService.applyVoucher(orderId, "SAVE10");

        assertThat(order.getVoucherCode()).isEqualTo("SAVE10");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("15000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("135000");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("15000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("135000");
    }

    @Test
    void payOrder_shouldDelegateToPaymentServiceAndUpdateTableStatus() {
        Long orderId = 3L;
        CafeTable table = CafeTable.builder().id(5L).name("T1").status(TableStatus.SERVING).build();
        Order paidOrder = buildPendingOrder(orderId);
        paidOrder.setStatus("PAID");
        paidOrder.setCafeTable(table);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("cash");

        when(paymentService.processPayment(orderId, request)).thenReturn(paidOrder);
        when(orderRepository.findPendingOrderByTableId(table.getId())).thenReturn(Optional.empty());
        when(cafeTableRepository.save(table)).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.payOrder(orderId, request);

        assertThat(response.getStatus()).isEqualTo("PAID");
        verify(paymentService).processPayment(orderId, request);
        verify(cafeTableRepository).save(table);
    }

    private Order buildPendingOrder(Long orderId) {
        Order order = Order.builder()
                .id(orderId)
                .status("PENDING")
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
