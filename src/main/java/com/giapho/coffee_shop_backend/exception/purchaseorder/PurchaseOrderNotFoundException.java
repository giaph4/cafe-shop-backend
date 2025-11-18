package com.giapho.coffee_shop_backend.exception.purchaseorder;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderNotFoundException extends BusinessException {

    public PurchaseOrderNotFoundException(Long purchaseOrderId) {
        super(HttpStatus.NOT_FOUND, "Purchase order not found with id: " + purchaseOrderId);
    }
}
