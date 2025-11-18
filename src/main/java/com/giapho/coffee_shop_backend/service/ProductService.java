package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> getFilteredProducts(String name, Long categoryId, Pageable pageable);

    ProductResponse getProductById(Long productId);

    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse updateProduct(Long productId, ProductRequest productRequest);

    void deleteProduct(Long productId);

    ProductResponse toggleProductAvailability(Long productId);

    ProductResponse createProductWithImage(ProductRequest productRequest, MultipartFile imageFile);

    ProductResponse updateProductWithImage(Long productId, ProductRequest productRequest, MultipartFile imageFile);

    ProductResponse deleteProductImage(Long productId);

    ProductResponse uploadProductImage(Long productId, MultipartFile imageFile);
}
