package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.InventoryAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<IngredientResponseDTO>> getAllIngredients(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 15, page = 0, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<IngredientResponseDTO> ingredients;
        if (name != null && !name.isEmpty()) {
            ingredients = ingredientService.searchIngredientsByName(name, pageable);
        } else {
            ingredients = ingredientService.getAllIngredients(pageable);
        }
        return ResponseEntity.ok(ingredients);
    }

    @GetMapping("/id")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<IngredientResponseDTO> getIngredientById(@PathVariable Long id) {
        IngredientResponseDTO ingredient = ingredientService.getIngredientById(id);
        return ResponseEntity.ok(ingredient);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<IngredientResponseDTO> createIngredient(@Valid @RequestBody IngredientRequestDTO request) {
        IngredientResponseDTO createdIngredient = ingredientService.createIngredient(request);
        return ResponseEntity.ok(createdIngredient);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<IngredientResponseDTO> updateIngredientInfo(
            @PathVariable Long id,
            @Valid @RequestBody IngredientRequestDTO request
    ) {
        IngredientResponseDTO updatedIngredient = ingredientService.updateIngredientInfo(id, request);
        return ResponseEntity.ok(updatedIngredient);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteIngredient(@PathVariable Long id) {
        ingredientService.deleteIngredient(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/adjust-inventory") // Endpoint mới
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<IngredientResponseDTO> adjustInventory(
            @Valid @RequestBody InventoryAdjustmentRequestDTO request
    ) {
        IngredientResponseDTO updatedIngredient = ingredientService.adjustInventory(request);
        return ResponseEntity.ok(updatedIngredient);
    }
}