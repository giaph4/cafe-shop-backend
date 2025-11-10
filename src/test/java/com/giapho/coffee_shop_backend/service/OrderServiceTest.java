package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.*;
import com.giapho.coffee_shop_backend.domain.repository.*;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.mapper.OrderDetailMapper;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CafeTableRepository cafeTableRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private ProductIngredientRepository productIngredientRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private VoucherService voucherService;
    @Mock
    private CustomerService customerService;

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
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));

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
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));

        OrderResponseDTO response = orderService.applyVoucher(orderId, "SAVE10");

        assertThat(order.getVoucherCode()).isEqualTo("SAVE10");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("15000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("135000");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("15000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("135000");
    }

    @Test
    void payOrder_shouldCompleteOrderAndSubtractInventory() {
        Long orderId = 3L;
        Long productId = 30L;
        Long ingredientId = 40L;

        CafeTable table = CafeTable.builder().id(5L).name("T1").status("SERVING").build();
        Customer customer = Customer.builder().id(6L).fullName("Loyal Customer").build();

        Order order = buildPendingOrder(orderId);
        order.setCafeTable(table);
        order.setCustomer(customer);
        order.setSubTotal(new BigDecimal("150000"));
        order.setTotalAmount(new BigDecimal("150000"));

        Product product = Product.builder().id(productId).name("Mocha").price(new BigDecimal("50000")).build();
        OrderDetail detail = OrderDetail.builder()
                .order(order)
                .product(product)
                .quantity(3)
                .priceAtOrder(product.getPrice())
                .build();
        order.getOrderDetails().add(detail);

        Ingredient ingredient = Ingredient.builder()
                .id(ingredientId)
                .name("Coffee Beans")
                .quantityOnHand(new BigDecimal("1000"))
                .unit("g")
                .build();

        ProductIngredient recipeItem = ProductIngredient.builder()
                .product(product)
                .ingredient(ingredient)
                .quantityNeeded(new BigDecimal("10"))
                .build();

        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));
        when(productIngredientRepository.findByProductId(productId)).thenReturn(List.of(recipeItem));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderRepository.findPendingOrderByTableId(table.getId())).thenReturn(Optional.empty());
        when(cafeTableRepository.save(table)).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findByIdWithDetails(orderId)).thenReturn(Optional.of(order));

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
        paymentRequest.setPaymentMethod("cash");

        OrderResponseDTO response = orderService.payOrder(orderId, paymentRequest);

        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getPaymentMethod()).isEqualTo("CASH");
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(ingredient.getQuantityOnHand()).isEqualByComparingTo("970");

        verify(customerService).updateLoyaltyPoints(customer.getId(), new BigDecimal("150000"));
        verify(orderRepository).save(order);
        verify(cafeTableRepository).save(table);

        assertThat(response.getStatus()).isEqualTo("PAID");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("150000");
    }

    @Test
    void payOrder_shouldThrowWhenPaymentMethodInvalid() {
        Long orderId = 4L;
        Order order = buildPendingOrder(orderId);
        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
        paymentRequest.setPaymentMethod("bitcoin");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.payOrder(orderId, paymentRequest));

        assertThat(exception.getMessage()).contains("Invalid payment method");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void payOrder_shouldFailWhenInventoryInsufficient() {
        Long orderId = 5L;
        Long productId = 50L;
        Long ingredientId = 60L;

        Order order = buildPendingOrder(orderId);
        order.setSubTotal(new BigDecimal("60000"));
        order.setTotalAmount(new BigDecimal("60000"));

        Product product = Product.builder().id(productId).name("Cappuccino").price(new BigDecimal("30000")).build();
        OrderDetail detail = OrderDetail.builder()
                .order(order)
                .product(product)
                .quantity(3)
                .priceAtOrder(product.getPrice())
                .build();
        order.getOrderDetails().add(detail);

        Ingredient ingredient = Ingredient.builder()
                .id(ingredientId)
                .name("Milk")
                .quantityOnHand(new BigDecimal("10"))
                .unit("ml")
                .build();

        ProductIngredient recipeItem = ProductIngredient.builder()
                .product(product)
                .ingredient(ingredient)
                .quantityNeeded(new BigDecimal("5"))
                .build();

        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));
        when(productIngredientRepository.findByProductId(productId)).thenReturn(List.of(recipeItem));
        when(ingredientRepository.findById(ingredientId)).thenReturn(Optional.of(ingredient));

        PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
        paymentRequest.setPaymentMethod("card");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> orderService.payOrder(orderId, paymentRequest));

        assertThat(exception.getMessage()).contains("Not enough stock");
        verify(orderRepository, never()).save(any());
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
