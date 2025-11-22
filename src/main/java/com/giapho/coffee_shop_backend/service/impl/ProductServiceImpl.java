package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductIngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.exception.category.CategoryNotFoundException;
import com.giapho.coffee_shop_backend.exception.product.InvalidProductDataException;
import com.giapho.coffee_shop_backend.exception.product.ProductCodeAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.product.ProductDeletionNotAllowedException;
import com.giapho.coffee_shop_backend.exception.product.ProductNotFoundException;
import com.giapho.coffee_shop_backend.config.CacheConfig;
import com.giapho.coffee_shop_backend.mapper.ProductMapper;
import com.giapho.coffee_shop_backend.service.FileStorageService;
import com.giapho.coffee_shop_backend.service.ProductService;
import com.giapho.coffee_shop_backend.util.SpecificationBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Locale NORMALIZE_LOCALE = Locale.ROOT;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductIngredientRepository productIngredientRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toProductResponse);
    }

    @Override
    public Page<ProductResponse> getFilteredProducts(String name, Long categoryId, Pageable pageable) {
        Specification<Product> specification = buildProductSpecification(name, categoryId);
        return productRepository.findAll(specification, pageable)
                .map(productMapper::toProductResponse);
    }

    @Override
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "#productId")
    public ProductResponse getProductById(Long productId) {
        Product product = findProduct(productId);
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest) {
        String normalizedCode = normalizeProductCode(productRequest.getCode());
        ensureProductCodeUnique(normalizedCode, null);

        Category category = findCategory(productRequest.getCategoryId());

        Product product = productMapper.toProduct(productRequest);
        product.setCategory(category);
        product.setCode(normalizedCode);
        product.setAvailable(true);

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)
    public ProductResponse updateProduct(Long productId, ProductRequest productRequest) {
        Product product = findProduct(productId);

        String normalizedCode = normalizeProductCode(productRequest.getCode());
        ensureProductCodeUnique(normalizedCode, productId);

        Category category = findCategory(productRequest.getCategoryId());

        productMapper.updateProductFromDto(productRequest, product);
        product.setCategory(category);
        product.setCode(normalizedCode);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)
    public void deleteProduct(Long productId) {
        Product product = findProduct(productId);

        if (orderDetailRepository.countByProductId(productId) > 0) {
            throw new ProductDeletionNotAllowedException(product.getName());
        }

        deleteImageFile(product.getImageUrl());
        productIngredientRepository.deleteByProductId(productId);
        productRepository.delete(product);

        log.info("Deleted product {} and associated resources", productId);
    }

    @Override
    @Transactional
    public ProductResponse toggleProductAvailability(Long productId) {
        Product product = findProduct(productId);
        product.setAvailable(!product.isAvailable());
        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse createProductWithImage(ProductRequest productRequest, MultipartFile imageFile) {
        String normalizedCode = normalizeProductCode(productRequest.getCode());
        ensureProductCodeUnique(normalizedCode, null);

        Category category = findCategory(productRequest.getCategoryId());

        Product product = productMapper.toProduct(productRequest);
        product.setCategory(category);
        product.setCode(normalizedCode);
        product.setAvailable(true);
        product.setImageUrl(storeImage(imageFile));

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProductWithImage(Long productId, ProductRequest productRequest, MultipartFile imageFile) {
        Product product = findProduct(productId);

        String normalizedCode = normalizeProductCode(productRequest.getCode());
        ensureProductCodeUnique(normalizedCode, productId);

        Category category = findCategory(productRequest.getCategoryId());

        productMapper.updateProductFromDto(productRequest, product);
        product.setCategory(category);
        product.setCode(normalizedCode);

        if (imageFile != null && !imageFile.isEmpty()) {
            replaceImage(product, imageFile);
        } else if (productRequest.getImageUrl() == null && StringUtils.hasText(product.getImageUrl())) {
            deleteImageFile(product.getImageUrl());
            product.setImageUrl(null);
        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse deleteProductImage(Long productId) {
        Product product = findProduct(productId);

        if (!StringUtils.hasText(product.getImageUrl())) {
            throw new InvalidProductDataException("Product does not have an image to delete");
        }

        deleteImageFile(product.getImageUrl());
        product.setImageUrl(null);

        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse uploadProductImage(Long productId, MultipartFile imageFile) {
        Product product = findProduct(productId);
        replaceImage(product, imageFile);
        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    private Specification<Product> buildProductSpecification(String name, Long categoryId) {
        if (categoryId != null) {
            ensureCategoryExists(categoryId);
        }

        return SpecificationBuilder.<Product>builder()
                .likeIgnoreCase("name", name)
                .equalNested("category", "id", categoryId)
                .build();
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private void ensureCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }

    private String normalizeProductCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new InvalidProductDataException("Product code must not be empty");
        }
        return code.trim().toUpperCase(NORMALIZE_LOCALE);
    }

    private void ensureProductCodeUnique(String normalizedCode, Long currentProductId) {
        Optional<Product> existingProduct = productRepository.findByCode(normalizedCode);
        if (existingProduct.isPresent() && !existingProduct.get().getId().equals(currentProductId)) {
            throw new ProductCodeAlreadyExistsException(normalizedCode);
        }
    }

    private void replaceImage(Product product, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new InvalidProductDataException("Image file is required");
        }
        deleteImageFile(product.getImageUrl());
        product.setImageUrl(storeImage(imageFile));
    }

    private String storeImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        String fileName = fileStorageService.storeFile(imageFile);
        log.info("Stored product image {}", fileName);
        return fileStorageService.getFileUrl(fileName);
    }

    private void deleteImageFile(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }
        try {
            String fileName = fileStorageService.extractFileNameFromUrl(imageUrl);
            if (StringUtils.hasText(fileName)) {
                fileStorageService.deleteFile(fileName);
                log.info("Deleted product image {}", fileName);
            }
        } catch (Exception ex) {
            log.warn("Could not delete image {}. Continuing without failing.", imageUrl, ex);
        }
    }
}
