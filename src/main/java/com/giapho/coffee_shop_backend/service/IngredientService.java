package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.InventoryAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    @Transactional(readOnly = true)
    public Page<IngredientResponseDTO> getAllIngredients(Pageable pageable) {
        Page<Ingredient> ingredientPage = ingredientRepository.findAll(pageable);
        return ingredientPage.map(ingredientMapper::entityToResponse);
    }

    @Transactional(readOnly = true)
    public Page<IngredientResponseDTO> searchIngredientsByName(String name, Pageable pageable) {
        Page<Ingredient> ingredientPage = ingredientRepository.findByNameContainingIgnoreCase(name, pageable);
        return ingredientPage.map(ingredientMapper::entityToResponse);
    }

    @Transactional(readOnly = true)
    public IngredientResponseDTO getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + id));
        return ingredientMapper.entityToResponse(ingredient);
    }

    @Transactional
    public IngredientResponseDTO createIngredient(IngredientRequestDTO request) {
        if (ingredientRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Ingredient name already exists: " + request.getName());
        }

        Ingredient newIngredient = ingredientMapper.toEntity(request);
        Ingredient savedIngredient = ingredientRepository.save(newIngredient);
        return ingredientMapper.entityToResponse(savedIngredient);
    }

    @Transactional
    public IngredientResponseDTO updateIngredientInfo(Long id, IngredientRequestDTO request) {
        Ingredient existingIngredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + id));

        if (!existingIngredient.getName().equals(request.getName()) &&
                ingredientRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Ingredient name already exists: " + request.getName());
        }

        ingredientMapper.updateEntityFromDto(request, existingIngredient);
        Ingredient updatedIngredient = ingredientRepository.save(existingIngredient);
        return ingredientMapper.entityToResponse(updatedIngredient);
    }

    @Transactional
    public void deleteIngredient(Long id) {
        if (!ingredientRepository.existsById(id)) {
            throw new EntityNotFoundException("Ingredient not found with id: " + id);
        }
        ingredientRepository.deleteById(id);
    }

    @Transactional
    public IngredientResponseDTO adjustInventory(InventoryAdjustmentRequestDTO request) {
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + request.getIngredientId()));

        BigDecimal oldQuantity = ingredient.getQuantityOnHand();
        BigDecimal newQuantity = request.getNewQuantityOnHand();

        ingredient.setQuantityOnHand(newQuantity);

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println(String.format(
                "[%s] Inventory Adjusted: Ingredient ID=%d (%s), Old Qty=%.3f, New Qty=%.3f, Reason: %s, By: %s",
                LocalDateTime.now(),
                ingredient.getId(),
                ingredient.getName(),
                oldQuantity,
                newQuantity,
                request.getReason() != null ? request.getReason() : "N/A",
                currentUser
        ));

        Ingredient updatedIngredient = ingredientRepository.save(ingredient);

        return ingredientMapper.entityToResponse(updatedIngredient);
    }
}
