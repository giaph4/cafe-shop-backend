package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.entity.ProductIngredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductIngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.dto.ProductIngredientDTO;
import com.giapho.coffee_shop_backend.dto.ProductRecipeDTO;
import com.giapho.coffee_shop_backend.exception.ingredient.IngredientNotFoundException;
import com.giapho.coffee_shop_backend.exception.product.ProductNotFoundException;
import com.giapho.coffee_shop_backend.exception.product.ProductRecipeEmptyException;
import com.giapho.coffee_shop_backend.exception.product.ProductRecipeInvalidIngredientException;
import com.giapho.coffee_shop_backend.mapper.ProductIngredientMapper;
import com.giapho.coffee_shop_backend.service.ProductRecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductRecipeServiceImpl implements ProductRecipeService {

    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final ProductIngredientMapper productIngredientMapper;

    @Override
    public List<ProductIngredientDTO> getRecipeByProductId(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        List<ProductIngredient> recipeItems = productIngredientRepository.findByProductId(product.getId());
        return productIngredientMapper.entityListToDtoList(recipeItems);
    }

    @Override
    @Transactional
    public List<ProductIngredientDTO> setRecipeForProduct(Long productId, ProductRecipeDTO recipeDTO) {
        if (recipeDTO == null || CollectionUtils.isEmpty(recipeDTO.getIngredients())) {
            throw new ProductRecipeEmptyException();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        List<ProductIngredientDTO> ingredientDTOs = recipeDTO.getIngredients();
        Map<Long, ProductIngredientDTO> ingredientById = buildUniqueIngredientMap(ingredientDTOs);
        Map<Long, Ingredient> ingredients = fetchIngredients(ingredientById.keySet());

        productIngredientRepository.deleteByProductId(productId);

        List<ProductIngredient> newRecipeItems = ingredientById.values().stream()
                .map(dto -> mapToEntity(product, dto, ingredients.get(dto.getIngredientId())))
                .toList();

        List<ProductIngredient> savedItems = productIngredientRepository.saveAll(newRecipeItems);
        log.info("Updated recipe for product {} with {} ingredients", productId, savedItems.size());

        return productIngredientMapper.entityListToDtoList(savedItems);
    }

    private Map<Long, ProductIngredientDTO> buildUniqueIngredientMap(List<ProductIngredientDTO> ingredientDTOs) {
        Map<Long, ProductIngredientDTO> result = new LinkedHashMap<>();
        for (ProductIngredientDTO dto : ingredientDTOs) {
            Long ingredientId = dto.getIngredientId();
            if (ingredientId == null) {
                throw new ProductRecipeInvalidIngredientException("Ingredient ID must be provided for all recipe items");
            }
            if (result.containsKey(ingredientId)) {
                throw new ProductRecipeInvalidIngredientException(
                        String.format("Duplicate ingredient detected in recipe: %d", ingredientId));
            }
            result.put(ingredientId, dto);
        }
        return result;
    }

    private Map<Long, Ingredient> fetchIngredients(Set<Long> ingredientIds) {
        List<Ingredient> ingredients = ingredientRepository.findAllById(ingredientIds);
        Map<Long, Ingredient> ingredientMap = ingredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        ingredientIds.forEach(id -> {
            if (!ingredientMap.containsKey(id)) {
                throw new IngredientNotFoundException(id);
            }
        });
        return ingredientMap;
    }

    private ProductIngredient mapToEntity(Product product, ProductIngredientDTO dto, Ingredient ingredient) {
        ProductIngredient entity = productIngredientMapper.dtoToEntity(dto);
        entity.setProduct(product);
        entity.setIngredient(ingredient);
        return entity;
    }
}
