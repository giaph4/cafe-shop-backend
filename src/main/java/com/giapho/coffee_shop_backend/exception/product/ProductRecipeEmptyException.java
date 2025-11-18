package com.giapho.coffee_shop_backend.exception.product;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to persist a recipe without ingredients.
 */
public class ProductRecipeEmptyException extends BusinessException {

    public ProductRecipeEmptyException() {
        super(HttpStatus.BAD_REQUEST, "Recipe must contain at least one ingredient");
    }
}
