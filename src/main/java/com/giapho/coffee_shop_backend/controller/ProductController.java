package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * API tạo sản phẩm mới
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        ProductResponse productResponse = productService.createProduct(productRequest);

        return ResponseEntity.ok(productResponse);
    }

    /**
     * API lấy tất cả sản phẩm (có phân trang)
     * Mọi nhân viên (STAFF, MANAGER, ADMIN) đều có quyền
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            // @PageableDefault: Cấu hình phân trang mặc định (size=10, page=0)
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * API lấy chi tiết sản phẩm
     * Mọi nhân viên (STAFF, MANAGER, ADMIN) đều có quyền
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * API cập nhật sản phẩm
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest productRequest
    ) {
        ProductResponse updatedProduct = productService.updateProduct(id, productRequest);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * API xoá sản phẩm
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API Ẩn/Hiện sản phẩm
     * Dùng @PatchMapping vì đây là cập nhật 1 phần
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @PatchMapping("/{id}/toggle-availability")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> toggleProductAvailability(@PathVariable Long id) {
        ProductResponse updatedProduct = productService.toggleProductAvailability(id);
        return ResponseEntity.ok(updatedProduct);
    }
}
