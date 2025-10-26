package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.InventoryAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    /**
     * Điều chỉnh số lượng tồn kho của một nguyên vật liệu
     * @param request DTO chứa ID nguyên liệu, số lượng mới và lý do
     * @return Thông tin nguyên vật liệu sau khi cập nhật
     */
    @Transactional
    public IngredientResponseDTO adjustInventory(InventoryAdjustmentRequestDTO request) {
        // 1. Tìm nguyên vật liệu
        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new EntityNotFoundException("Ingredient not found with id: " + request.getIngredientId()));

        // 2. Lấy số lượng cũ (để ghi log nếu cần)
        BigDecimal oldQuantity = ingredient.getQuantityOnHand();
        BigDecimal newQuantity = request.getNewQuantityOnHand();

        // 3. Cập nhật số lượng tồn kho mới
        ingredient.setQuantityOnHand(newQuantity);

        // 4. (Tùy chọn) Ghi log về việc điều chỉnh này
        // Có thể tạo một bảng riêng 'inventory_logs' hoặc ghi vào log file thông thường
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName(); // Lấy user hiện tại
        System.out.println(String.format(
                "[%s] Inventory Adjusted: Ingredient ID=%d (%s), Old Qty=%.3f, New Qty=%.3f, Reason: %s, By: %s",
                LocalDateTime.now(),
                ingredient.getId(),
                ingredient.getName(),
                oldQuantity,
                newQuantity,
                request.getReason() != null ? request.getReason() : "N/A",
                currentUser
        ));

        // 5. Lưu thay đổi
        Ingredient updatedIngredient = ingredientRepository.save(ingredient);

        // 6. Trả về DTO
        return ingredientMapper.entityToResponse(updatedIngredient);
    }
}
