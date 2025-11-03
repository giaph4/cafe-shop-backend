package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.ProductIngredientDTO;
import com.giapho.coffee_shop_backend.dto.ProductRecipeDTO;
import com.giapho.coffee_shop_backend.service.ProductRecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Đặt URL dựa trên sản phẩm: /api/v1/products/{productId}/recipe
@RequestMapping("/api/v1/products/{productId}/recipe")
@RequiredArgsConstructor
public class ProductRecipeController {

    private final ProductRecipeService productRecipeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProductIngredientDTO>> getRecipe(@PathVariable Long productId) {
        List<ProductIngredientDTO> recipe = productRecipeService.getRecipeByProductId(productId);
        return ResponseEntity.ok(recipe);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<ProductIngredientDTO>> setRecipe(
            @PathVariable Long productId,
            @Valid @RequestBody ProductRecipeDTO recipeDTO
    ) {
        List<ProductIngredientDTO> updatedRecipe = productRecipeService.setRecipeForProduct(productId, recipeDTO);
        return ResponseEntity.ok(updatedRecipe);
    }
}