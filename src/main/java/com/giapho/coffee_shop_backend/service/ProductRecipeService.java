package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.ProductIngredientDTO;
import com.giapho.coffee_shop_backend.dto.ProductRecipeDTO;

import java.util.List;

public interface ProductRecipeService {

    List<ProductIngredientDTO> getRecipeByProductId(Long productId);

    List<ProductIngredientDTO> setRecipeForProduct(Long productId, ProductRecipeDTO recipeDTO);
}