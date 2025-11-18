package com.giapho.coffee_shop_backend.exception.purchaseorder;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderStatusException extends BusinessException {

    public PurchaseOrderStatusException(String action, String currentStatus) {
        super(HttpStatus.CONFLICT, String.format("Cannot %s purchase order in status %s", action, currentStatus));
    }
}
