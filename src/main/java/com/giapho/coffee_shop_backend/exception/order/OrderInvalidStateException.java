package com.giapho.coffee_shop_backend.exception.order;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an order action is rejected due to an invalid state.
 */
public class OrderInvalidStateException extends BusinessException {

    public OrderInvalidStateException(Long orderId, String state) {
        super(HttpStatus.BAD_REQUEST,
                String.format("Order %d is not in a valid state for this action: %s", orderId, state));
    }

    public OrderInvalidStateException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
