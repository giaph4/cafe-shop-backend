package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.dto.CategoryDTO;
import com.giapho.coffee_shop_backend.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CATEGORIES_CACHE = "categories";

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @CacheEvict(cacheNames = CATEGORIES_CACHE, allEntries = true)
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        String name = categoryDTO.getName() == null ? "" : categoryDTO.getName().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name must not be empty");
        }

        if (categoryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category with name " + name + " already exists");
        }

        Category category = categoryMapper.toCategory(categoryDTO);
        category.setName(name);

        Category saved = categoryRepository.save(category);

        return categoryMapper.toCategoryDTO(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CATEGORIES_CACHE, key = "'all'")
    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categoryMapper.toCategoryDTOs(categories);
    }

    @Transactional
    @CacheEvict(cacheNames = CATEGORIES_CACHE, allEntries = true)
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

    @CacheEvict(cacheNames = CATEGORIES_CACHE, allEntries = true)
    public void deleteCategory(Long id) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category with id " + id + " not found"));

        categoryRepository.delete(existingCategory);
    }
}
