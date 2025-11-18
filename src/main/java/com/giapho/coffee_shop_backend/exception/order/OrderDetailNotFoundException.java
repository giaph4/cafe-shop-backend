package com.giapho.coffee_shop_backend.exception.order;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a specific order detail is missing in a given order.
 */
public class OrderDetailNotFoundException extends BusinessException {

    public OrderDetailNotFoundException(Long orderId, Long detailId) {
        super(HttpStatus.NOT_FOUND,
                String.format("Order detail %d not found in order %d", detailId, orderId));
    }
}
