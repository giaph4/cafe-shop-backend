package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class ProductRecipeDTO {

    // Không cần productId ở đây vì nó sẽ là path variable trong API

    @NotEmpty(message = "Recipe must contain at least one ingredient")
    @Valid // Validate các item bên trong list
    private List<ProductIngredientDTO> ingredients; // Danh sách các nguyên liệu cần thiết
}