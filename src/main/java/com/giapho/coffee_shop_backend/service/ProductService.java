package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductIngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.mapper.ProductMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductIngredientRepository productIngredientRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final FileStorageService fileStorageService;

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
     * Lấy sản phẩm có lọc theo tên và/hoặc categoryId
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFilteredProducts(String name, Long categoryId, Pageable pageable) {

        Specification<Product> spec = (root, query, criteriaBuilder) -> null;

        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }

        if (categoryId != null) {
            // Tùy chọn: Kiểm tra Category có tồn tại không
            if (categoryId > 0 && !categoryRepository.existsById(categoryId)) { // Giả sử ID > 0 là ID hợp lệ
                throw new EntityNotFoundException("Category not found with id: " + categoryId);
            }
            // Thêm điều kiện lọc
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        // --- Execute Query ---
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return productPage.map(productMapper::toProductResponse);
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
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        long orderDetailCount = orderDetailRepository.countByProductId(id);
        if (orderDetailCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete product '" + product.getName() +
                            "' because it exists in past order details. Consider marking it as unavailable instead.");
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                String fileName = fileStorageService.extractFileNameFromUrl(product.getImageUrl());
                fileStorageService.deleteFile(fileName);
                log.info("Product image deleted during product deletion: {}", fileName);
            } catch (Exception e) {
                log.error("Failed to delete product image: {}", product.getImageUrl(), e);
            }
        }

        // Xóa product ingredients
        productIngredientRepository.deleteByProductId(id);
        productIngredientRepository.flush();

        // Xóa product
        productRepository.deleteById(id);

        log.info("Deleted product and its data for ID: {}", id);
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

    @Transactional
    public ProductResponse createProductWithImage(
            ProductRequest productRequest,
            MultipartFile imageFile
    ) {
        // Validate unique code
        if (productRepository.existsByCode(productRequest.getCode())) {
            throw new IllegalArgumentException("Product code already exists: " + productRequest.getCode());
        }

        // Validate category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + productRequest.getCategoryId()));

        // Upload image nếu có
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            imageUrl = fileStorageService.getFileUrl(fileName);
            log.info("Product image uploaded: {}", fileName);
        }

        // Tạo product
        Product product = productMapper.toProduct(productRequest);
        product.setCategory(category);
        product.setAvailable(true);
        product.setImageUrl(imageUrl); // Set image URL

        Product savedProduct = productRepository.save(product);

        return productMapper.toProductResponse(savedProduct);
    }

    /**
     * CẢI TIẾN: Cập nhật sản phẩm với image upload (optional)
     */
    @Transactional
    public ProductResponse updateProductWithImage(
            Long id,
            ProductRequest productRequest,
            MultipartFile imageFile
    ) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        // Validate category
        Category category = categoryRepository.findById(productRequest.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found with id: " + productRequest.getCategoryId()));

        // Lưu old image URL để xóa sau (nếu cần)
        String oldImageUrl = existingProduct.getImageUrl();

        // Upload image mới nếu có
        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            String newImageUrl = fileStorageService.getFileUrl(fileName);

            // Xóa file cũ nếu có
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    String oldFileName = fileStorageService.extractFileNameFromUrl(oldImageUrl);
                    fileStorageService.deleteFile(oldFileName);
                    log.info("Old product image deleted: {}", oldFileName);
                } catch (Exception e) {
                    log.error("Failed to delete old image: {}", oldImageUrl, e);
                    // Không throw exception, chỉ log
                }
            }

            existingProduct.setImageUrl(newImageUrl);
            log.info("Product image updated: {}", fileName);
        }

        // Update product info
        productMapper.updateProductFromDto(productRequest, existingProduct);
        existingProduct.setCategory(category);

        Product updatedProduct = productRepository.save(existingProduct);



        return productMapper.toProductResponse(updatedProduct);
    }

    /**
     * CẢI TIẾN: Xóa image của product
     */
    @Transactional
    public ProductResponse deleteProductImage(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        String imageUrl = product.getImageUrl();

        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new IllegalArgumentException("Product does not have an image");
        }

        try {
            String fileName = fileStorageService.extractFileNameFromUrl(imageUrl);
            fileStorageService.deleteFile(fileName);
            log.info("Product image deleted: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete product image: {}", imageUrl, e);
            throw new RuntimeException("Failed to delete product image", e);
        }

        product.setImageUrl(null);
        Product updatedProduct = productRepository.save(product);

        return productMapper.toProductResponse(updatedProduct);
    }


    /**
     * Upload/Update chỉ image cho product đã tồn tại
     */
    @Transactional
    public ProductResponse uploadProductImage(Long id, MultipartFile imageFile) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        // Lưu old image URL để xóa sau
        String oldImageUrl = product.getImageUrl();

        // Upload image mới
        String fileName = fileStorageService.storeFile(imageFile);
        String newImageUrl = fileStorageService.getFileUrl(fileName);

        // Xóa file cũ nếu có
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            try {
                String oldFileName = fileStorageService.extractFileNameFromUrl(oldImageUrl);
                fileStorageService.deleteFile(oldFileName);
                log.info("Old product image replaced: {}", oldFileName);
            } catch (Exception e) {
                log.error("Failed to delete old image: {}", oldImageUrl, e);
            }
        }

        product.setImageUrl(newImageUrl);
        Product updatedProduct = productRepository.save(product);


        log.info("Product image uploaded successfully: {}", fileName);
        return productMapper.toProductResponse(updatedProduct);
    }
}
