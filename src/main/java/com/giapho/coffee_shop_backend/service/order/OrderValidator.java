package com.giapho.coffee_shop_backend.service.order;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.OrderType;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.exception.customer.CustomerNotFoundException;
import com.giapho.coffee_shop_backend.exception.order.OrderDetailNotFoundException;
import com.giapho.coffee_shop_backend.exception.order.OrderInvalidStateException;
import com.giapho.coffee_shop_backend.exception.order.OrderNotFoundException;
import com.giapho.coffee_shop_backend.exception.order.TableNotFoundException;
import com.giapho.coffee_shop_backend.exception.product.ProductNotFoundException;
import com.giapho.coffee_shop_backend.exception.product.ProductUnavailableException;
import com.giapho.coffee_shop_backend.exception.user.UserNotAuthenticatedException;
import com.giapho.coffee_shop_backend.exception.voucher.VoucherInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderValidator {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CafeTableRepository cafeTableRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UserNotAuthenticatedException();
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new OrderInvalidStateException("Authenticated user not found: " + username));
    }

    public Customer resolveCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    public CafeTable validateTableForNewOrder(Long tableId) {
        if (tableId == null) {
            return null;
        }
        CafeTable table = cafeTableRepository.findById(tableId)
                .orElseThrow(() -> new TableNotFoundException(tableId));

        if (table.getStatus() == TableStatus.EMPTY) {
            return table;
        }

        Optional<Order> pendingOrder = orderRepository.findByTableIdAndStatus(tableId, OrderStatus.PENDING);
        if (pendingOrder.isPresent()) {
            Order order = pendingOrder.get();
            throw new OrderInvalidStateException(
                    String.format("Table %s already has pending order #%d", table.getName(), order.getId()));
        }

        throw new OrderInvalidStateException(
                String.format("Table %s is currently %s and cannot receive a new order", table.getName(), table.getStatus()));
    }

    public Order requirePendingOrder(Long orderId) {
        return orderRepository.findByIdAndStatusWithDetails(orderId, OrderStatus.PENDING)
                .orElseThrow(() -> new OrderNotFoundException("Pending order not found with id: " + orderId));
    }

    public Order requireOrderWithDetails(Long orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public Product requireAvailableProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        if (!product.isAvailable()) {
            throw new ProductUnavailableException(productId);
        }
        return product;
    }

    public OrderDetail requireOrderDetail(Order order, Long orderDetailId) {
        return order.getOrderDetails().stream()
                .filter(detail -> orderDetailId.equals(detail.getId()))
                .findFirst()
                .orElseThrow(() -> new OrderDetailNotFoundException(order.getId(), orderDetailId));
    }

    public String normalizeVoucherCode(String voucherCode) {
        if (!StringUtils.hasText(voucherCode)) {
            throw new VoucherInvalidException("", "Voucher code must not be blank");
        }
        return voucherCode.trim().toUpperCase();
    }

    public OrderType parseOrderType(String type) {
        if (!StringUtils.hasText(type)) {
            return OrderType.DINE_IN;
        }
        try {
            return OrderType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new OrderInvalidStateException("Unsupported order type: " + type);
        }
    }

    public OrderStatus parseOrderStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new OrderInvalidStateException("Order status must not be blank");
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new OrderInvalidStateException("Unsupported order status: " + status);
        }
    }
}
