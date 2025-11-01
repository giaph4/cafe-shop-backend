package com.giapho.coffee_shop_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    
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
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        Page<ProductResponse> products = productService.getFilteredProducts(name, categoryId, pageable);
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

    /**
     * ENDPOINT MỚI: Tạo sản phẩm với image upload
     * Sử dụng multipart/form-data
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProductWithImage(
            @RequestPart("product") String productJson, // JSON string
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            // Parse JSON string thành ProductRequest
            ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);

            // Validate manually (vì không dùng @Valid với @RequestPart String)
            // Hoặc bạn có thể dùng Validator bean

            ProductResponse productResponse = productService.createProductWithImage(productRequest, imageFile);
            return ResponseEntity.ok(productResponse);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse product data", e);
        }
    }

    /**
     * ENDPOINT MỚI: Cập nhật sản phẩm với image upload (optional)
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProductWithImage(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            ProductRequest productRequest = objectMapper.readValue(productJson, ProductRequest.class);
            ProductResponse updatedProduct = productService.updateProductWithImage(id, productRequest, imageFile);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse product data", e);
        }
    }

    /**
     * ENDPOINT MỚI: Xóa image của product
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> deleteProductImage(@PathVariable Long id) {
        ProductResponse updatedProduct = productService.deleteProductImage(id);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * ENDPOINT MỚI: Chỉ upload/update image cho product đã tồn tại
     * Chỉ MANAGER hoặc ADMIN mới có quyền
     */
    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> uploadProductImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile imageFile
    ) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        ProductResponse updatedProduct = productService.uploadProductImage(id, imageFile);
        return ResponseEntity.ok(updatedProduct);
    }
}
