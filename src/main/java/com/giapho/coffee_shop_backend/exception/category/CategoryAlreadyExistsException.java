package com.giapho.coffee_shop_backend.exception.category;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CategoryAlreadyExistsException extends BusinessException {

    public CategoryAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, String.format("Category with name '%s' already exists", name));
    }
}
