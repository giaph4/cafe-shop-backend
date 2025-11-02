// src/main/java/com/giapho/coffee_shop_backend/mapper/ProductMapper.java
package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category", target = "categoryName", qualifiedByName = "categoryToCategoryName")
    ProductResponse toProductResponse(Product product);

    @Mapping(source = "categoryId", target = "category", qualifiedByName = "categoryIdToCategory")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isAvailable", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toProduct(ProductRequest productRequest);

    /**
     * SỬA LỖI: Thêm @Mapping(target = "imageUrl", ignore = true)
     * Để Mapper không ghi đè URL ảnh mà chúng ta xử lý thủ công trong Service.
     */
    @Mapping(source = "categoryId", target = "category", qualifiedByName = "categoryIdToCategory")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "available", ignore = true)
    // Sửa lỗi: Tên trường là "isAvailable" trong Entity, không phải "available"
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    // <-- DÒNG SỬA LỖI QUAN TRỌNG
    void updateProductFromDto(ProductRequest dto, @MappingTarget Product product);

    // --- Các hàm Helper (Named) ---

    @Named("categoryToCategoryName")
    default String categoryToCategoryName(Category category) {
        if (category == null) {
            return null;
        }
        return category.getName();
    }

    @Named("categoryIdToCategory")
    default Category categoryIdToCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryId);
        return category;
    }
}