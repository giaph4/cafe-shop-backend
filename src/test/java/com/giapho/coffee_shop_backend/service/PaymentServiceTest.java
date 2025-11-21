package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.entity.ProductIngredient;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductIngredientRepository;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import com.giapho.coffee_shop_backend.exception.inventory.InsufficientInventoryException;
import com.giapho.coffee_shop_backend.exception.order.PaymentMethodInvalidException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
import com.giapho.coffee_shop_backend.service.impl.PaymentServiceImpl;
import com.giapho.coffee_shop_backend.service.order.OrderPricingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private ProductIngredientRepository productIngredientRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private VoucherService voucherService;
    @Mock
    private OrderPricingService orderPricingService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void processPayment_shouldCompleteOrderAndSubtractInventory() {
        Long orderId = 100L;
        Long productId = 200L;
        Long ingredientId = 300L;

        Order order = buildPendingOrder(orderId);
        Customer customer = Customer.builder().id(400L).fullName("Vip Customer").build();
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
        when(ingredientRepository.findByIdForUpdate(ingredientId)).thenReturn(Optional.of(ingredient));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("cash");

        Order result = paymentService.processPayment(orderId, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentMethod()).isEqualTo("CASH");
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(ingredient.getQuantityOnHand()).isEqualByComparingTo("970");
        assertThat(result).isSameAs(order);

        verify(orderRepository).save(order);
        verify(customerService).updateLoyaltyPoints(customer.getId(), new BigDecimal("150000"));
        verify(orderPricingService).recalculateTotals(order);
        verify(ingredientRepository).save(ingredient);
    }

    @Test
    void processPayment_shouldAttachCustomerFromRequestWhenMissing() {
        Long orderId = 101L;
        Long customerId = 501L;

        Order order = buildPendingOrder(orderId);
        order.setSubTotal(new BigDecimal("50000"));
        order.setTotalAmount(new BigDecimal("50000"));

        Customer customer = Customer.builder().id(customerId).fullName("New Customer").build();

        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.save(order)).thenReturn(order);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setCustomerId(customerId);
        request.setPaymentMethod("card");

        paymentService.processPayment(orderId, request);

        assertThat(order.getCustomer()).isEqualTo(customer);
        assertThat(order.getPaymentMethod()).isEqualTo("CARD");
        verify(orderPricingService).recalculateTotals(order);
    }

    @Test
    void processPayment_shouldApplyVoucherAndIncrementUsage() {
        Long orderId = 104L;
        String voucherCode = "SAVE20";

        Order order = buildPendingOrder(orderId);
        order.setSubTotal(new BigDecimal("200000"));
        order.setDiscountAmount(BigDecimal.ZERO);

        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        VoucherCheckResponseDTO responseDTO = VoucherCheckResponseDTO.builder()
                .isValid(true)
                .discountAmount(new BigDecimal("30000"))
                .message("OK")
                .code(voucherCode)
                .build();

        when(voucherService.checkAndCalculateDiscount(eq(voucherCode), eq(new BigDecimal("200000"))))
                .thenReturn(responseDTO);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("cash");
        request.setVoucherCode(voucherCode);

        paymentService.processPayment(orderId, request);

        assertThat(order.getVoucherCode()).isEqualTo(voucherCode);
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("30000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("170000");

        verify(voucherService).checkAndCalculateDiscount(voucherCode, new BigDecimal("200000"));
        verify(voucherService).incrementUsageCount(voucherCode);
        verify(orderPricingService).recalculateTotals(order);
    }

    @Test
    void processPayment_shouldRejectInvalidVoucher() {
        Long orderId = 105L;
        String voucherCode = "EXPIRED";

        Order order = buildPendingOrder(orderId);
        order.setSubTotal(new BigDecimal("80000"));

        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));

        VoucherCheckResponseDTO invalid = VoucherCheckResponseDTO.builder()
                .isValid(false)
                .message("Voucher đã hết hạn.")
                .code(voucherCode)
                .discountAmount(BigDecimal.ZERO)
                .build();

        when(voucherService.checkAndCalculateDiscount(eq(voucherCode), eq(new BigDecimal("80000"))))
                .thenReturn(invalid);

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("cash");
        request.setVoucherCode(voucherCode);

        assertThrows(VoucherInvalidException.class, () -> paymentService.processPayment(orderId, request));

        verify(orderRepository, never()).save(any());
        verify(voucherService, never()).incrementUsageCount(any());
    }

    @Test
    void processPayment_shouldThrowWhenPaymentMethodInvalid() {
        Long orderId = 102L;
        Order order = buildPendingOrder(orderId);

        when(orderRepository.findByIdWithCustomer(orderId)).thenReturn(Optional.of(order));

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("bitcoin");

        assertThrows(PaymentMethodInvalidException.class,
                () -> paymentService.processPayment(orderId, request));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void processPayment_shouldFailWhenInventoryInsufficient() {
        Long orderId = 103L;
        Long productId = 203L;
        Long ingredientId = 303L;

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
        when(ingredientRepository.findByIdForUpdate(ingredientId)).thenReturn(Optional.of(ingredient));

        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setPaymentMethod("transfer");

        InsufficientInventoryException exception = assertThrows(InsufficientInventoryException.class,
                () -> paymentService.processPayment(orderId, request));

        assertThat(exception.getMessage()).contains("Not enough stock");
        verify(orderRepository, never()).save(any());
    }

    private Order buildPendingOrder(Long orderId) {
        return Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .orderDetails(new java.util.HashSet<>())
                .build();
    }
}
