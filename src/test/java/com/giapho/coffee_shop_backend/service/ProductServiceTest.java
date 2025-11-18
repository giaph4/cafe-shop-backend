package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductIngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.dto.ProductRequest;
import com.giapho.coffee_shop_backend.dto.ProductResponse;
import com.giapho.coffee_shop_backend.exception.product.InvalidProductDataException;
import com.giapho.coffee_shop_backend.mapper.ProductMapper;
import com.giapho.coffee_shop_backend.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductIngredientRepository productIngredientRepository;
    @Mock
    private OrderDetailRepository orderDetailRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createProduct_shouldNormalizeCodeBeforePersisting() {
        ProductRequest request = ProductRequest.builder()
                .name("Latte")
                .code(" prd001 ")
                .price(new BigDecimal("45000"))
                .categoryId(5L)
                .build();

        Category category = new Category();
        category.setId(5L);
        category.setName("Coffee");

        Product mappedProduct = Product.builder()
                .name("Latte")
                .price(new BigDecimal("45000"))
                .build();

        when(productRepository.findByCode("PRD001")).thenReturn(Optional.empty());
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productMapper.toProduct(request)).thenReturn(mappedProduct);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toProductResponse(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .code(product.getCode())
                    .price(product.getPrice())
                    .available(product.isAvailable())
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .build();
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getCode()).isEqualTo("PRD001");
        assertThat(response.isAvailable()).isTrue();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertThat(saved.getCode()).isEqualTo("PRD001");
        assertThat(saved.isAvailable()).isTrue();
        verify(productRepository).findByCode("PRD001");
    }

    @Test
    void createProduct_shouldRejectBlankCode() {
        ProductRequest request = ProductRequest.builder()
                .name("Cappuccino")
                .code("   ")
                .price(new BigDecimal("50000"))
                .categoryId(1L)
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(InvalidProductDataException.class)
                .hasMessageContaining("Product code must not be empty");
    }
}
