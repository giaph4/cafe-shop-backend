package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    /**
     * Lấy danh sách nguyên vật liệu (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<IngredientResponseDTO> getAllIngredients(Pageable pageable) {
        Page<Ingredient> ingredientPage = ingredientRepository.findAll(pageable);
        return ingredientPage.map(ingredientMapper::entityToResponse);
    }

    /**
     * Tìm kiếm nguyên vật liệu theo tên (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<IngredientResponseDTO> searchIngredientsByName(String name, Pageable pageable) {
        Page<Ingredient> ingredientPage = ingredientRepository.findByNameContainingIgnoreCase(name, pageable);
        return ingredientPage.map(ingredientMapper::entityToResponse);
    }

    /**
     * Lấy chi tiết một nguyên vật liệu
     */
    @Transactional(readOnly = true)
    public IngredientResponseDTO getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + id));
        return ingredientMapper.entityToResponse(ingredient);
    }

    /**
     * Tạo nguyên vật liệu mới
     */
    @Transactional
    public IngredientResponseDTO createIngredient(IngredientRequestDTO request) {
        if (ingredientRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Ingredient name already exists: " + request.getName());
        }

        Ingredient newIngredient = ingredientMapper.toEntity(request);
        // quantityOnHand mặc định là 0 (do mapper và entity)
        Ingredient savedIngredient = ingredientRepository.save(newIngredient);
        return ingredientMapper.entityToResponse(savedIngredient);
    }

    /**
     * Cập nhật thông tin nguyên vật liệu (tên, đơn vị, ngưỡng cảnh báo)
     * Không cập nhật tồn kho qua đây.
     */
    @Transactional
    public IngredientResponseDTO updateIngredientInfo(Long id, IngredientRequestDTO request) {
        Ingredient existingIngredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + id));

        // Kiểm tra tên mới (nếu thay đổi)
        if (!existingIngredient.getName().equals(request.getName()) &&
                ingredientRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Ingredient name already exists: " + request.getName());
        }

        // Dùng mapper cập nhật (mapper đã ignore quantityOnHand)
        ingredientMapper.updateEntityFromDto(request, existingIngredient);
        Ingredient updatedIngredient = ingredientRepository.save(existingIngredient);
        return ingredientMapper.entityToResponse(updatedIngredient);
    }

    /**
     * Xoá nguyên vật liệu
     */
    @Transactional
    public void deleteIngredient(Long id) {
        if (!ingredientRepository.existsById(id)) {
            throw new EntityNotFoundException("Ingredient not found with id: " + id);
        }
        // (Cần kiểm tra xem Ingredient có đang được sử dụng trong ProductIngredient
        // hoặc PurchaseOrderDetail nào không trước khi xoá)
        // (Tạm thời cho phép xoá)
        ingredientRepository.deleteById(id);
    }

    // --- CÁC NGHIỆP VỤ LIÊN QUAN TỒN KHO SẼ THÊM SAU ---
    // Ví dụ: Cập nhật tồn kho khi nhập hàng, trừ kho khi bán hàng...
}
