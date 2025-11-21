package com.giapho.coffee_shop_backend.exception.ingredient;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class IngredientNameAlreadyExistsException extends BusinessException {

    public IngredientNameAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, String.format("Nguyên liệu với tên '%s' đã tồn tại.", name));
    }
}
