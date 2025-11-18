package com.giapho.coffee_shop_backend.exception.order;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an order cannot be located by given identifier.
 */
public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(Long orderId) {
        super(HttpStatus.NOT_FOUND, String.format("Order not found with id: %d", orderId));
    }

    public OrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
