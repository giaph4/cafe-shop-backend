package com.giapho.coffee_shop_backend.exception.ingredient;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an ingredient cannot be located.
 */
public class IngredientNotFoundException extends BusinessException {

    public IngredientNotFoundException(Long ingredientId) {
        super(HttpStatus.NOT_FOUND, String.format("Ingredient not found with id: %d", ingredientId));
    }
}
