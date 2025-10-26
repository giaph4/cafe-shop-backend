package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface IngredientMapper {

    // DTO -> Entity (tạo mới)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantityOnHand", ignore = true)
    Ingredient toEntity(IngredientRequestDTO dto);

    // Entity -> DTO (hiển thị)
    IngredientResponseDTO entityToResponse(Ingredient entity);

    // Cập nhật Entity từ DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quantityOnHand", ignore = true)
    void updateEntityFromDto(IngredientRequestDTO dto, @MappingTarget Ingredient entity);
}
