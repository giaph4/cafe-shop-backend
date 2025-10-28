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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductIngredientRepository productIngredientRepository;
    private final OrderDetailRepository orderDetailRepository;

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
        // 1. Kiểm tra sản phẩm tồn tại
        Product product = productRepository.findById(id) // Lấy cả đối tượng Product để lấy tên
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        // 2. Kiểm tra xem sản phẩm có trong bất kỳ OrderDetail nào không (QUAN TRỌNG)
        long orderDetailCount = orderDetailRepository.countByProductId(id); // Giả sử bạn thêm hàm countByProductId vào OrderDetailRepository
        if (orderDetailCount > 0) {
            throw new IllegalArgumentException("Cannot delete product '" + product.getName() + "' because it exists in past order details. Consider marking it as unavailable instead.");
            // Hoặc bạn có thể dùng một Exception tùy chỉnh khác
        }

        // 3. Xóa các dòng công thức liên quan (nếu sản phẩm không có trong order details)
        productIngredientRepository.deleteByProductId(id);
        productIngredientRepository.flush(); // Optional but safer

        // 4. Bây giờ mới xóa sản phẩm
        productRepository.deleteById(id);

        System.out.println("Deleted product and its ingredients for ID: " + id); // Log (tùy chọn)
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
