package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrderDetail;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderDetailResponseDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface PurchaseOrderDetailMapper {

    // RequestDTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchaseOrder", ignore = true) // Sẽ gán thủ công
    @Mapping(source = "ingredientId", target = "ingredient", qualifiedByName = "ingredientIdToIngredient")
    PurchaseOrderDetail requestToEntity(PurchaseOrderDetailRequestDTO dto);

    // Entity -> ResponseDTO
    @Mapping(source = "ingredient.id", target = "ingredientId")
    @Mapping(source = "ingredient.name", target = "ingredientName")
    @Mapping(source = "ingredient.unit", target = "ingredientUnit")
    @Mapping(target = "lineTotal", ignore = true) // Sẽ tính sau khi map
    PurchaseOrderDetailResponseDTO entityToResponse(PurchaseOrderDetail entity);

    Set<PurchaseOrderDetailResponseDTO> entitySetToResponseSet(Set<PurchaseOrderDetail> details);

    // Hàm tính thành tiền sau khi map xong
    @AfterMapping
    default void calculateLineTotal(@MappingTarget PurchaseOrderDetailResponseDTO target, PurchaseOrderDetail source) {
        if (source.getQuantity() != null && source.getUnitPrice() != null) {
            target.setLineTotal(source.getQuantity().multiply(source.getUnitPrice()));
        } else {
            target.setLineTotal(BigDecimal.ZERO);
        }
    }

    // Helper tạo Ingredient proxy
    @Named("ingredientIdToIngredient")
    default Ingredient ingredientIdToIngredient(Long ingredientId) {
        if (ingredientId == null) return null;
        Ingredient ingredient = new Ingredient();
        ingredient.setId(ingredientId);
        return ingredient;
    }
}