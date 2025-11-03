package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.ProductIngredient;
import com.giapho.coffee_shop_backend.dto.ProductIngredientDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductIngredientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(source = "ingredientId", target = "ingredient", qualifiedByName = "ingredientIdToIngredient")
    ProductIngredient dtoToEntity(ProductIngredientDTO dto);

    @Mapping(source = "ingredient.id", target = "ingredientId")
    @Mapping(source = "ingredient.name", target = "ingredientName")
    @Mapping(source = "ingredient.unit", target = "ingredientUnit")
    ProductIngredientDTO entityToDto(ProductIngredient entity);

    List<ProductIngredientDTO> entityListToDtoList(List<ProductIngredient> entities);

    @Named("ingredientIdToIngredient")
    default Ingredient ingredientIdToIngredient(Long ingredientId) {
        if (ingredientId == null) return null;
        Ingredient ingredient = new Ingredient();
        ingredient.setId(ingredientId);
        return ingredient;
    }
}