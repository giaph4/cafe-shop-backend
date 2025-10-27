package com.giapho.coffee_shop_backend.domain.repository;

import com.giapho.coffee_shop_backend.domain.entity.Product;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    // Tìm tất cả sản phẩm thuộc một danh mục (theo categoryId)
    // Dùng Pageable để phân trang
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Tìm kiếm sản phẩm theo tên (không phân biệt hoa thường)
    // Và lọc theo trạng thái isAvailable
    // Dùng Pageable để phân trang
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.isAvailable = :available")
    Page<Product> searchByNameAndAvailability(String keyword, boolean available, Pageable pageable);


    // Lấy tất cả sản phẩm đang "available" (dùng cho khách hàng)
    Page<Product> findByIsAvailable(boolean isAvailable, Pageable pageable);



    boolean existsByCode(@NotBlank(message = "Product code is required") String code);
}
