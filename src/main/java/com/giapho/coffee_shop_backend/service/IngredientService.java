package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.InventoryAdjustmentRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IngredientService {

    Page<IngredientResponseDTO> getAllIngredients(Pageable pageable);

    Page<IngredientResponseDTO> searchIngredientsByName(String name, Pageable pageable);

    IngredientResponseDTO getIngredientById(Long id);

    IngredientResponseDTO createIngredient(IngredientRequestDTO request);

    IngredientResponseDTO updateIngredientInfo(Long id, IngredientRequestDTO request);

    void deleteIngredient(Long id);

    IngredientResponseDTO adjustInventory(InventoryAdjustmentRequestDTO request);
}
