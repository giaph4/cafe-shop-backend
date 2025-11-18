package com.giapho.coffee_shop_backend.exception.purchaseorder;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderValidationException extends BusinessException {

    public PurchaseOrderValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
