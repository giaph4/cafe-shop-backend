package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    // Tìm tất cả định lượng cho một sản phẩm cụ thể
    List<ProductIngredient> findByProductId(Long productId);

    // (Tùy chọn) Tìm định lượng cụ thể cho một sản phẩm và một nguyên liệu
    // Optional<ProductIngredient> findByProductIdAndIngredientId(Long productId, Long ingredientId);

    // (Tùy chọn) Xóa tất cả định lượng của một sản phẩm (dùng khi cập nhật công thức)
    void deleteByProductId(Long productId);
}