package com.giapho.coffee_shop_backend.service.impl;

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
import com.giapho.coffee_shop_backend.exception.customer.CustomerNotFoundException;
import com.giapho.coffee_shop_backend.exception.ingredient.IngredientNotFoundException;
import com.giapho.coffee_shop_backend.exception.inventory.InsufficientInventoryException;
import com.giapho.coffee_shop_backend.exception.order.OrderInvalidStateException;
import com.giapho.coffee_shop_backend.exception.order.OrderNotFoundException;
import com.giapho.coffee_shop_backend.exception.order.PaymentMethodInvalidException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
import com.giapho.coffee_shop_backend.service.CustomerService;
import com.giapho.coffee_shop_backend.service.PaymentService;
import com.giapho.coffee_shop_backend.service.VoucherService;
import com.giapho.coffee_shop_backend.service.order.OrderPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String CASH = "CASH";
    private static final String TRANSFER = "TRANSFER";
    private static final String CARD = "CARD";

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final CustomerService customerService;
    private final VoucherService voucherService;
    private final OrderPricingService orderPricingService;

    @Override
    @Transactional
    public Order processPayment(Long orderId, PaymentRequestDTO paymentRequest) {
        Order order = loadOrder(orderId);
        ensureOrderPending(orderId, order);

        attachCustomerIfNecessary(order, paymentRequest.getCustomerId());
        String paymentMethod = normalizePaymentMethod(paymentRequest.getPaymentMethod());

        applyVoucherDetails(order, paymentRequest.getVoucherCode());

        subtractInventoryForOrder(order);
        orderPricingService.recalculateTotals(order);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);

        updateCustomerLoyaltyPoints(order);

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} paid successfully with payment method: {}", orderId, paymentMethod);

        incrementVoucherUsageIfNecessary(order);
        return savedOrder;
    }

    private Order loadOrder(Long orderId) {
        return orderRepository.findByIdWithCustomer(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void ensureOrderPending(Long orderId, Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderInvalidStateException(orderId, order.getStatus().name());
        }
    }

    private void attachCustomerIfNecessary(Order order, Long customerId) {
        if (customerId == null || order.getCustomer() != null) {
            return;
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        order.setCustomer(customer);
        log.info("Associated customer ID {} with order {} during payment", customerId, order.getId());
    }

    private String normalizePaymentMethod(String paymentMethodInput) {
        if (!StringUtils.hasText(paymentMethodInput)) {
            throw new PaymentMethodInvalidException("Payment method is required.");
        }

        String normalized = paymentMethodInput.trim().toUpperCase();
        if (CASH.equals(normalized) || TRANSFER.equals(normalized) || CARD.equals(normalized)) {
            return normalized;
        }

        throw new PaymentMethodInvalidException("Invalid payment method. Supported methods: CASH, TRANSFER, CARD");
    }

    private void applyVoucherDetails(Order order, String voucherCode) {
        BigDecimal subTotal = defaultZero(order.getSubTotal());

        if (!StringUtils.hasText(voucherCode)) {
            ensureTotalsWhenVoucherMissing(order, subTotal);
            return;
        }

        String normalizedVoucherCode = voucherCode.trim().toUpperCase();
        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(normalizedVoucherCode, subTotal);

        if (!voucherCheck.isValid()) {
            throw new VoucherInvalidException(normalizedVoucherCode, voucherCheck.getMessage());
        }

        BigDecimal discountAmount = defaultZero(voucherCheck.getDiscountAmount());
        BigDecimal recalculatedTotal = subTotal.subtract(discountAmount);
        if (recalculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
            recalculatedTotal = BigDecimal.ZERO;
        }

        order.setVoucherCode(normalizedVoucherCode);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(recalculatedTotal);

        log.info("Validated voucher {} for order {}. Discount: {}",
                normalizedVoucherCode,
                order.getId(),
                discountAmount);
    }

    private void ensureTotalsWhenVoucherMissing(Order order, BigDecimal subTotal) {
        if (order.getDiscountAmount() == null) {
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        if (order.getTotalAmount() != null) {
            return;
        }

        BigDecimal discountAmount = defaultZero(order.getDiscountAmount());
        BigDecimal recalculatedTotal = subTotal.subtract(discountAmount);
        if (recalculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
            recalculatedTotal = BigDecimal.ZERO;
        }
        order.setTotalAmount(recalculatedTotal);
    }

    private void subtractInventoryForOrder(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return;
        }

        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail == null || detail.getProduct() == null) {
                continue;
            }
            deductIngredients(detail.getProduct(), detail.getQuantity());
        }
    }

    private void deductIngredients(Product product, int orderQuantity) {
        if (orderQuantity <= 0) {
            log.debug("Skipping inventory deduction for product {} because quantity is {}", product.getId(), orderQuantity);
            return;
        }

        List<ProductIngredient> recipe = productIngredientRepository.findByProductId(product.getId());
        if (recipe.isEmpty()) {
            log.warn("No recipe found for product ID: {}. Skipping stock deduction.", product.getId());
            return;
        }

        for (ProductIngredient recipeItem : recipe) {
            if (recipeItem.getIngredient() == null) {
                continue;
            }

            BigDecimal quantityNeededPerProduct = defaultZero(recipeItem.getQuantityNeeded());
            BigDecimal totalQuantityToSubtract = quantityNeededPerProduct.multiply(BigDecimal.valueOf(orderQuantity));
            updateIngredientInventory(recipeItem.getIngredient().getId(), totalQuantityToSubtract);
        }
    }

    private void updateIngredientInventory(Long ingredientId, BigDecimal quantityToSubtract) {
        Ingredient currentIngredient = ingredientRepository.findByIdForUpdate(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));

        BigDecimal currentStock = defaultZero(currentIngredient.getQuantityOnHand());
        if (currentStock.compareTo(quantityToSubtract) < 0) {
            throw new InsufficientInventoryException(
                    currentIngredient.getName(),
                    quantityToSubtract.toPlainString(),
                    currentStock.toPlainString());
        }

        currentIngredient.setQuantityOnHand(currentStock.subtract(quantityToSubtract));
        ingredientRepository.save(currentIngredient);
    }

    private void updateCustomerLoyaltyPoints(Order order) {
        if (order.getCustomer() == null) {
            log.warn("No customer associated with order {}. Cannot add loyalty points.", order.getId());
            return;
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid total amount {} for order {}. Cannot add loyalty points.", order.getTotalAmount(), order.getId());
            return;
        }

        try {
            log.info("Attempting to add loyalty points for customer {} with amount {}",
                    order.getCustomer().getId(), order.getTotalAmount());
            customerService.updateLoyaltyPoints(order.getCustomer().getId(), order.getTotalAmount());
            log.info("Successfully updated loyalty points for customer {}", order.getCustomer().getId());
        } catch (Exception e) {
            log.error("Failed to update loyalty points for customer: {}", order.getCustomer().getId(), e);
        }
    }

    private void incrementVoucherUsageIfNecessary(Order order) {
        if (!StringUtils.hasText(order.getVoucherCode())) {
            return;
        }
        voucherService.incrementUsageCount(order.getVoucherCode());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
