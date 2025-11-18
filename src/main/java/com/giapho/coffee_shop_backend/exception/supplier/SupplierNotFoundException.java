package com.giapho.coffee_shop_backend.exception.supplier;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SupplierNotFoundException extends BusinessException {

    public SupplierNotFoundException(Long supplierId) {
        super(HttpStatus.NOT_FOUND, "Supplier not found with id: " + supplierId);
    }

    public SupplierNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
