package com.giapho.coffee_shop_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.exception.product.ProductDataParsingException;
import com.giapho.coffee_shop_backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * Create a new product. Supports both JSON (application/json) and
     * multipart/form-data with image upload.
     *
     * For JSON: Send ProductRequest in body For Multipart: Send "product" as
     * JSON string and "image" as file
     */
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody(required = false) ProductRequest productRequest,
            @RequestPart(value = "product", required = false) String productJson,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            // Handle multipart form data
            if (productJson != null) {
                ProductRequest parsedRequest = objectMapper.readValue(productJson, ProductRequest.class);
                ProductResponse response = productService.createProductWithImage(parsedRequest, imageFile);
                return ResponseEntity.ok(response);
            }

            // Handle JSON body
            if (productRequest != null) {
                ProductResponse response = productService.createProduct(productRequest);
                return ResponseEntity.ok(response);
            }

            throw new ProductDataParsingException("Product data is required");

        } catch (JsonProcessingException e) {
            throw new ProductDataParsingException("Failed to parse product data", e);
        }
    }

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Update an existing product. Supports both JSON (application/json) and
     * multipart/form-data with image upload.
     *
     * For JSON: Send ProductRequest in body For Multipart: Send "product" as
     * JSON string and "image" as file
     */
    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "product", required = false) String productJson,
            @RequestPart(value = "request", required = false) @Valid ProductRequest productRequest,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            // Handle multipart form data
            if (productJson != null) {
                ProductRequest parsedRequest = objectMapper.readValue(productJson, ProductRequest.class);
                ProductResponse response = productService.updateProductWithImage(id, parsedRequest, imageFile);
                return ResponseEntity.ok(response);
            }

            // Handle JSON body
            if (productRequest != null) {
                ProductResponse response = productService.updateProduct(id, productRequest);
                return ResponseEntity.ok(response);
            }

            throw new ProductDataParsingException("Product data is required");

        } catch (JsonProcessingException e) {
            throw new ProductDataParsingException("Failed to parse product data", e);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-availability")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> toggleProductAvailability(@PathVariable Long id) {
        ProductResponse updatedProduct = productService.toggleProductAvailability(id);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductResponse> deleteProductImage(@PathVariable Long id) {
        ProductResponse updatedProduct = productService.deleteProductImage(id);
        return ResponseEntity.ok(updatedProduct);
    }

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
