package com.giapho.coffee_shop_backend.service.report.core;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import com.giapho.coffee_shop_backend.service.report.helper.ReportDateValidator;
import com.giapho.coffee_shop_backend.service.report.helper.ReportCalculationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryReportProvider {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public List<IngredientResponseDTO> getCurrentInventory() {
        return mapIngredients(ingredientRepository.findAll());
    }

    public List<IngredientResponseDTO> getLowStockIngredients() {
        return mapIngredients(ingredientRepository.findIngredientsBelowReorderLevel());
    }

    public int getLowStockCount() {
        return ingredientRepository.findIngredientsBelowReorderLevel().size();
    }

    public BigDecimal getTotalImportedIngredientCost(LocalDate startDate, LocalDate endDate) {
        ReportDateValidator.validateOptionalRange(startDate, endDate);
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        return ReportCalculationHelper.defaultZero(
                purchaseOrderRepository.sumTotalAmountByStatusAndOptionalDateRange("COMPLETED", start, end)
        );
    }

    private List<IngredientResponseDTO> mapIngredients(List<Ingredient> ingredients) {
        return ingredients.stream()
                .map(ingredientMapper::entityToResponse)
                .collect(Collectors.toList());
    }
}
