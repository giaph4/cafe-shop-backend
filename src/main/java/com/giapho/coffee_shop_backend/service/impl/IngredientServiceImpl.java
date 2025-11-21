package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.InventoryAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.audit.AuditLogRequest;
import com.giapho.coffee_shop_backend.exception.ingredient.IngredientNameAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.ingredient.IngredientNotFoundException;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import com.giapho.coffee_shop_backend.service.AuditLogService;
import com.giapho.coffee_shop_backend.service.IngredientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientServiceImpl implements IngredientService {

    private static final int MAX_UNIT_LENGTH = 10;

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<IngredientResponseDTO> getAllIngredients(Pageable pageable) {
        return ingredientRepository.findAll(pageable)
                .map(ingredientMapper::entityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IngredientResponseDTO> searchIngredientsByName(String name, Pageable pageable) {
        if (!StringUtils.hasText(name)) {
            return getAllIngredients(pageable);
        }
        return ingredientRepository.findByNameContainingIgnoreCase(name.trim(), pageable)
                .map(ingredientMapper::entityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public IngredientResponseDTO getIngredientById(Long id) {
        Ingredient ingredient = findIngredient(id);
        return ingredientMapper.entityToResponse(ingredient);
    }

    @Override
    @Transactional
    public IngredientResponseDTO createIngredient(IngredientRequestDTO request) {
        String normalizedName = normalizeName(request.getName());
        validateNameUniqueness(normalizedName, null);
        String normalizedUnit = normalizeUnit(request.getUnit());

        Ingredient ingredient = ingredientMapper.toEntity(request);
        ingredient.setName(normalizedName);
        ingredient.setUnit(normalizedUnit);
        ingredient.setReorderLevel(defaultZero(request.getReorderLevel()));
        if (ingredient.getQuantityOnHand() == null) {
            ingredient.setQuantityOnHand(BigDecimal.ZERO);
        }

        Ingredient saved = ingredientRepository.save(ingredient);
        log.info("Created ingredient {} (ID={})", normalizedName, saved.getId());
        return ingredientMapper.entityToResponse(saved);
    }

    @Override
    @Transactional
    public IngredientResponseDTO updateIngredientInfo(Long id, IngredientRequestDTO request) {
        Ingredient ingredient = findIngredient(id);

        String normalizedName = normalizeName(request.getName());
        if (!ingredient.getName().equalsIgnoreCase(normalizedName)) {
            validateNameUniqueness(normalizedName, ingredient.getId());
        }

        ingredientMapper.updateEntityFromDto(request, ingredient);
        ingredient.setName(normalizedName);
        ingredient.setUnit(normalizeUnit(request.getUnit()));
        ingredient.setReorderLevel(defaultZero(request.getReorderLevel()));

        Ingredient saved = ingredientRepository.save(ingredient);
        log.info("Updated ingredient {} (ID={})", normalizedName, id);
        return ingredientMapper.entityToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteIngredient(Long id) {
        Ingredient ingredient = findIngredient(id);
        ingredientRepository.delete(ingredient);
        log.info("Deleted ingredient {} (ID={})", ingredient.getName(), id);
    }

    @Override
    @Transactional
    public IngredientResponseDTO adjustInventory(InventoryAdjustmentRequestDTO request) {
        Long ingredientId = request.getIngredientId();
        Ingredient ingredient = ingredientRepository.findByIdForUpdate(ingredientId)
                .orElseGet(() -> handleInventoryAdjustmentNotFound(request));

        BigDecimal oldQuantity = defaultZero(ingredient.getQuantityOnHand());
        BigDecimal newQuantity = defaultZero(request.getNewQuantityOnHand());

        ingredient.setQuantityOnHand(newQuantity);
        Ingredient saved = ingredientRepository.save(ingredient);

        auditLogService.recordSuccess(AuditLogRequest.builder()
                .action("INGREDIENT_INVENTORY_ADJUSTED")
                .resourceType("INGREDIENT")
                .resourceId(ingredientId.toString())
                .success(true)
                .summary(buildSuccessSummary(ingredient))
                .details(buildSuccessDetails(oldQuantity, newQuantity, request.getReason()))
                .build());

        log.info("Adjusted inventory for ingredient {} (ID={}) from {} to {}",
                ingredient.getName(), ingredientId, oldQuantity.toPlainString(), newQuantity.toPlainString());
        return ingredientMapper.entityToResponse(saved);
    }

    private Ingredient handleInventoryAdjustmentNotFound(InventoryAdjustmentRequestDTO request) {
        Long ingredientId = request.getIngredientId();
        IngredientNotFoundException exception = new IngredientNotFoundException(ingredientId);
        auditLogService.recordFailure(AuditLogRequest.builder()
                .action("INGREDIENT_INVENTORY_ADJUSTMENT_FAILED")
                .resourceType("INGREDIENT")
                .resourceId(ingredientId.toString())
                .success(false)
                .summary(String.format("Inventory adjustment failed for ingredient ID=%s", ingredientId))
                .errorMessage(exception.getMessage())
                .build());
        throw exception;
    }

    private Ingredient findIngredient(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
    }

    private void validateNameUniqueness(String normalizedName, Long currentId) {
        boolean exists = ingredientRepository.existsByNameIgnoreCase(normalizedName);
        if (!exists) {
            return;
        }
        if (currentId == null) {
            throw new IngredientNameAlreadyExistsException(normalizedName);
        }
        ingredientRepository.findByName(normalizedName)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IngredientNameAlreadyExistsException(normalizedName);
                });
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Tên nguyên liệu không được để trống.");
        }
        return name.trim();
    }

    private String normalizeUnit(String unit) {
        if (!StringUtils.hasText(unit)) {
            throw new IllegalArgumentException("Đơn vị không được để trống.");
        }
        String normalized = unit.trim();
        if (normalized.length() > MAX_UNIT_LENGTH) {
            throw new IllegalArgumentException("Đơn vị vượt quá độ dài cho phép.");
        }
        return normalized;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String buildSuccessSummary(Ingredient ingredient) {
        return String.format("Inventory adjusted for ingredient ID=%s (%s)",
                ingredient.getId(), ingredient.getName());
    }

    private String buildSuccessDetails(BigDecimal oldQuantity, BigDecimal newQuantity, String reason) {
        String sanitizedReason = reason == null ? "" : reason.replace("\"", "\\\"");
        return String.format("{\"oldQuantity\":\"%s\",\"newQuantity\":\"%s\",\"reason\":\"%s\"}",
                oldQuantity.toPlainString(),
                newQuantity.toPlainString(),
                sanitizedReason);
    }
}
