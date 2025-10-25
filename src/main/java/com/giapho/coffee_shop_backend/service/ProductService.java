package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.mapper.ProductMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    /**
     * Lấy sản phẩm (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        // 2. Dùng hàm map() của Page để chuyển Page<Entity> -> Page<DTO>
        // productMapper::toProductResponse là một "method reference"
        return productPage.map(productMapper::toProductResponse);
    }

    /**
     * Lấy chi tiết 1 sản phẩm
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id " + productId));

        return productMapper.toProductResponse(product);
    }

    /**
     * Tạo sản phẩm mới
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        if (productRepository.existsByCode(productRequest.getCode())) {
            throw new IllegalArgumentException("Product code already exists: " + productRequest.getCode());
        }

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + productRequest.getCategoryId()));

        Product product = productMapper.toProduct(productRequest);
        System.out.println(">>> DEBUG: Set isAvailable to: " + product.isAvailable());
        product.setCategory(category);
        product.setAvailable(true);

        System.out.println(">>> DEBUG: Set isAvailable to: " + product.isAvailable());

        Product savedProduct = productRepository.save(product);

        return productMapper.toProductResponse(savedProduct);
    }

    /**
     * Cập nhật sản phẩm
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Category not found with id: " + productRequest.getCategoryId()));

        productMapper.updateProductFromDto(productRequest, existingProduct);

        existingProduct.setCategory(category);

        Product updatedProduct = productRepository.save(existingProduct);

        return productMapper.toProductResponse(updatedProduct);
    }

    /**
     * Xoá sản phẩm
     */
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    /**
     * Ẩn/hiện sản phẩm
     */
    @Transactional
    public ProductResponse toggleProductAvailability(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        product.setAvailable(!product.isAvailable());

        Product updateProduct = productRepository.save(product);

        return productMapper.toProductResponse(updateProduct);
    }

    /**
     * Tạo bàn mới
     */

}
