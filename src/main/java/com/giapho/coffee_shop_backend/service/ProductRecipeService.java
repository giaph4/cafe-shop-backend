package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.entity.ProductIngredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductIngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.dto.ProductIngredientDTO;
import com.giapho.coffee_shop_backend.dto.ProductRecipeDTO;
// Import mapper nếu bạn tạo ở bước trước
// import com.giapho.coffee_shop_backend.mapper.ProductIngredientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductRecipeService {

    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    // Inject mapper nếu bạn tạo
    // private final ProductIngredientMapper productIngredientMapper;

    /**
     * Lấy công thức (định lượng) của một sản phẩm
     */
    @Transactional(readOnly = true)
    public List<ProductIngredientDTO> getRecipeByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found with id: " + productId);
        }
        List<ProductIngredient> ingredients = productIngredientRepository.findByProductId(productId);

        // Map thủ công Entity -> DTO
        return ingredients.stream()
                .map(pi -> {
                    ProductIngredientDTO dto = new ProductIngredientDTO();
                    dto.setId(pi.getId());
                    dto.setIngredientId(pi.getIngredient().getId());
                    dto.setIngredientName(pi.getIngredient().getName()); // Lấy tên từ Ingredient
                    dto.setIngredientUnit(pi.getIngredient().getUnit());   // Lấy đơn vị từ Ingredient
                    dto.setQuantityNeeded(pi.getQuantityNeeded());
                    return dto;
                })
                .collect(Collectors.toList());

        // Hoặc dùng mapper nếu bạn đã tạo:
        // return productIngredientMapper.entityListToDtoList(ingredients);
    }

    /**
     * Cập nhật (ghi đè) toàn bộ công thức cho một sản phẩm
     */
    @Transactional
    public List<ProductIngredientDTO> setRecipeForProduct(Long productId, ProductRecipeDTO recipeDTO) {
        // 1. Tìm Product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));

        // 2. Xóa công thức cũ của sản phẩm này
        productIngredientRepository.deleteByProductId(productId);
        // Quan trọng: Cần flush để lệnh delete thực thi trước khi insert mới,
        // hoặc đảm bảo ProductIngredient được xóa khỏi collection của Product nếu có
        productIngredientRepository.flush(); // Đảm bảo lệnh xóa thực thi ngay

        // 3. Tạo danh sách ProductIngredient mới từ DTO
        List<ProductIngredient> newRecipeItems = new ArrayList<>();
        for (ProductIngredientDTO itemDTO : recipeDTO.getIngredients()) {
            // Kiểm tra Ingredient tồn tại
            Ingredient ingredient = ingredientRepository.findById(itemDTO.getIngredientId())
                    .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + itemDTO.getIngredientId()));

            ProductIngredient pi = ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .quantityNeeded(itemDTO.getQuantityNeeded())
                    .build();
            newRecipeItems.add(pi);

            // Hoặc dùng mapper nếu bạn đã tạo:
            // ProductIngredient pi = productIngredientMapper.dtoToEntity(itemDTO);
            // pi.setProduct(product); // Gán Product cha
            // pi.setIngredient(ingredient); // Gán Ingredient đầy đủ (mapper chỉ tạo proxy)
            // newRecipeItems.add(pi);
        }

        // 4. Lưu tất cả các dòng định lượng mới
        List<ProductIngredient> savedItems = productIngredientRepository.saveAll(newRecipeItems);

        // 5. Map kết quả đã lưu sang DTO để trả về
        return savedItems.stream()
                .map(pi -> {
                    ProductIngredientDTO dto = new ProductIngredientDTO();
                    dto.setId(pi.getId());
                    dto.setIngredientId(pi.getIngredient().getId());
                    dto.setIngredientName(pi.getIngredient().getName());
                    dto.setIngredientUnit(pi.getIngredient().getUnit());
                    dto.setQuantityNeeded(pi.getQuantityNeeded());
                    return dto;
                })
                .collect(Collectors.toList());

        // Hoặc dùng mapper nếu bạn đã tạo:
        // return productIngredientMapper.entityListToDtoList(savedItems);
    }
}