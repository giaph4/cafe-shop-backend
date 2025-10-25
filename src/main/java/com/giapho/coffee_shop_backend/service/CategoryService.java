package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.dto.CategoryDTO;
import com.giapho.coffee_shop_backend.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Tạo danh mục mới
     */
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        String name = categoryDTO.getName() == null ? "" : categoryDTO.getName().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name must not be empty");
        }

        if (categoryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with name " + name + " already exists");
        }

        Category category = categoryMapper.toCategory(categoryDTO);
        category.setName(name); // đảm bảo tên đã được chuẩn hóa

        Category saved = categoryRepository.save(category);

        return categoryMapper.toCategoryDTO(saved);
    }

    /**
     * Lấy tất cả danh mục
     */
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categoryMapper.toCategoryDTOs(categories);
    }

    /**
     * Cập nhật danh mục
     */
    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with id " + id + " not found"));

        if (!existingCategory.getName().equals(categoryDTO.getName()) &&
                categoryRepository.existsByName(categoryDTO.getName())) {
            throw new IllegalArgumentException("Category name already exists");
        }

        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());

        Category updateCategory = categoryRepository.save(existingCategory);

        return categoryMapper.toCategoryDTO(updateCategory);
    }

    /**
     * Xoá danh mục
     */
    public void deleteCategory(Long id) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with id " + id + " not found"));

        categoryRepository.delete(existingCategory);
    }
}
