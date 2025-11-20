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
import com.giapho.coffee_shop_backend.exception.customer.CustomerNotFoundException;
import com.giapho.coffee_shop_backend.exception.ingredient.IngredientNotFoundException;
import com.giapho.coffee_shop_backend.exception.inventory.InsufficientInventoryException;
import com.giapho.coffee_shop_backend.exception.order.OrderInvalidStateException;
import com.giapho.coffee_shop_backend.exception.order.OrderNotFoundException;
import com.giapho.coffee_shop_backend.exception.order.PaymentMethodInvalidException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
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
public class PaymentService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final CustomerService customerService;
    private final VoucherService voucherService;
    private final OrderPricingService orderPricingService;

    @Transactional
    public Order processPayment(Long orderId, PaymentRequestDTO paymentRequest) {
        Order order = orderRepository.findByIdWithCustomer(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderInvalidStateException(orderId, order.getStatus().name());
        }

        String paymentMethod = validatePaymentMethod(paymentRequest.getPaymentMethod());

        if (paymentRequest.getCustomerId() != null && order.getCustomer() == null) {
            Customer customer = customerRepository.findById(paymentRequest.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException(paymentRequest.getCustomerId()));
            order.setCustomer(customer);
            log.info("Associated customer ID {} with order {} during payment", customer.getId(), orderId);
        }

        BigDecimal orderSubTotal = order.getSubTotal() != null ? order.getSubTotal() : BigDecimal.ZERO;

        if (StringUtils.hasText(paymentRequest.getVoucherCode())) {
            String normalizedVoucherCode = paymentRequest.getVoucherCode().trim().toUpperCase();
            VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(normalizedVoucherCode, orderSubTotal);

            if (!voucherCheck.isValid()) {
                throw new VoucherInvalidException(normalizedVoucherCode, voucherCheck.getMessage());
            }

            BigDecimal discountAmount = voucherCheck.getDiscountAmount() != null ? voucherCheck.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal recalculatedTotal = orderSubTotal.subtract(discountAmount);
            if (recalculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
                recalculatedTotal = BigDecimal.ZERO;
            }

            order.setVoucherCode(normalizedVoucherCode);
            order.setDiscountAmount(discountAmount);
            order.setTotalAmount(recalculatedTotal);

            log.info("Validated voucher {} for order {}. Discount: {}", normalizedVoucherCode, orderId, discountAmount);
        } else {
            if (order.getDiscountAmount() == null) {
                order.setDiscountAmount(BigDecimal.ZERO);
            }

            if (order.getTotalAmount() == null) {
                BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal recalculatedTotal = orderSubTotal.subtract(discountAmount);
                if (recalculatedTotal.compareTo(BigDecimal.ZERO) < 0) {
                    recalculatedTotal = BigDecimal.ZERO;
                }
                order.setTotalAmount(recalculatedTotal);
            }
        }

        subtractInventoryForOrder(order);
        orderPricingService.recalculateTotals(order);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);

        updateCustomerLoyaltyPoints(order);

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} paid successfully with payment method: {}", orderId, paymentMethod);

        if (StringUtils.hasText(order.getVoucherCode())) {
            voucherService.incrementUsageCount(order.getVoucherCode());
        }

        return savedOrder;
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
            // Không nên để lỗi này làm hỏng giao dịch thanh toán chính
        }
    }

    private void subtractInventoryForOrder(Order order) {
        if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
            return;
        }

        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product == null) {
                continue;
            }
            int orderQuantity = detail.getQuantity();
            if (orderQuantity <= 0) {
                log.debug("Skipping inventory deduction for product {} because quantity is {}", product.getId(), orderQuantity);
                continue;
            }

            List<ProductIngredient> recipe = productIngredientRepository.findByProductId(product.getId());

            if (recipe.isEmpty()) {
                log.warn("No recipe found for product ID: {}. Skipping stock deduction.", product.getId());
                continue;
            }

            for (ProductIngredient pi : recipe) {
                Ingredient ingredient = pi.getIngredient();
                if (ingredient == null) {
                    continue;
                }

                BigDecimal quantityNeededPerProduct = pi.getQuantityNeeded();
                BigDecimal totalQuantityToSubtract = quantityNeededPerProduct.multiply(BigDecimal.valueOf(orderQuantity));

                Ingredient currentIngredient = ingredientRepository.findByIdForUpdate(ingredient.getId())
                        .orElseThrow(() -> new IngredientNotFoundException(ingredient.getId()));

                BigDecimal currentStock = currentIngredient.getQuantityOnHand();

                if (currentStock.compareTo(totalQuantityToSubtract) < 0) {
                    throw new InsufficientInventoryException(
                            currentIngredient.getName(),
                            totalQuantityToSubtract.toPlainString(),
                            currentStock.toPlainString());
                }

                currentIngredient.setQuantityOnHand(currentStock.subtract(totalQuantityToSubtract));
                ingredientRepository.save(currentIngredient);
            }
        }
    }

    private String validatePaymentMethod(String paymentMethodInput) {
        if (!StringUtils.hasText(paymentMethodInput)) {
            throw new PaymentMethodInvalidException("Payment method is required.");
        }
        String paymentMethod = paymentMethodInput.trim().toUpperCase();
        if (!paymentMethod.equals("CASH") && !paymentMethod.equals("TRANSFER") && !paymentMethod.equals("CARD")) {
            throw new PaymentMethodInvalidException("Invalid payment method. Supported methods: CASH, TRANSFER, CARD");
        }
        return paymentMethod;
    }
}
