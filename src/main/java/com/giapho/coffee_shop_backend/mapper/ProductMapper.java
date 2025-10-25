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

    /**
     * Chuyển từ Entity (Product) sang DTO (ProductResponse)
     * Chúng ta cần ánh xạ đặc biệt cho trường 'categoryName'
     */
    @Mapping(source = "category", target = "categoryName", qualifiedByName = "categoryToCategoryName" )
//    @Mapping(source = "isAvailable", target = "available")
    ProductResponse toProductResponse(Product product);

    /**
     * Chuyển từ DTO (ProductRequest) sang Entity (Product)
     * Chúng ta cần ánh xạ đặc biệt cho trường 'category'
     */
    @Mapping(source = "categoryId", target = "category", qualifiedByName = "categoryIdToCategory" )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isAvailable", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toProduct(ProductRequest productRequest);

    /**
     * Nó sẽ lấy thông tin từ DTO và cập nhật vào Entity đã tồn tại.
     * Chúng ta @Mapping(target = "...") cho các trường không muốn bị DTO ghi đè.
     */
    @Mapping(source = "categoryId", target = "category", qualifiedByName = "categoryIdToCategory" )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateProductFromDto(ProductRequest dto, @MappingTarget Product product);

    // --- Các hàm Helper (Named) ---

    /**
     * Hàm helper: MapStruct sẽ dùng hàm này khi thấy
     * @Mapping(..., qualifiedByName = "categoryToCategoryName")
     */
    @Named("categoryToCategoryName")
    default String categoryToCategoryName(Category category) {
        if (category == null) {
            return null;
        }
        return category.getName();
    }

    /**
     * Hàm helper: MapStruct sẽ dùng hàm này khi thấy
     * @Mapping(..., qualifiedByName = "categoryIdToCategory")
     * Hàm này chỉ tạo 1 đối tượng Category "giả" (proxy)
     * chứa ID. JPA sẽ hiểu và tự động gán mối quan hệ
     * mà không cần truy vấn Category từ DB.
     */
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
