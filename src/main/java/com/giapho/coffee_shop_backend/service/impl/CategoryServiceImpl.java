package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.dto.category.CategoryCreateRequest;
import com.giapho.coffee_shop_backend.dto.category.CategoryResponse;
import com.giapho.coffee_shop_backend.dto.category.CategoryUpdateRequest;
import com.giapho.coffee_shop_backend.exception.category.CategoryAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.category.CategoryNotFoundException;
import com.giapho.coffee_shop_backend.exception.category.CategoryValidationException;
import com.giapho.coffee_shop_backend.mapper.CategoryMapper;
import com.giapho.coffee_shop_backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private static final String CATEGORIES_CACHE = "categories";

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @CacheEvict(cacheNames = CATEGORIES_CACHE, allEntries = true)
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        String normalizedName = normalizeName(request.getName());
        validateNameUniqueness(normalizedName, null);

        Category category = categoryMapper.toEntity(request);
        if (category == null) {
            category = new Category();
        }
        category.setName(normalizedName);
        category.setDescription(normalizeNullable(request.getDescription()));

        Category saved = categoryRepository.save(category);
        log.info("Created category '{}' with id {}", saved.getName(), saved.getId());

        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CATEGORIES_CACHE, key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        return categoryMapper.toResponseList(categories);
    }

    @Override
    @CacheEvict(cacheNames = CATEGORIES_CACHE, allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        updateCategoryFields(category, request);
        validateNameUniqueness(category.getName(), id);

        Category saved = categoryRepository.save(category);
        log.info("Updated category '{}' (id={})", saved.getName(), saved.getId());

        return categoryMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(cacheNames = CATEGORIES_CACHE, allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        categoryRepository.delete(category);
        log.info("Deleted category '{}' (id={})", category.getName(), category.getId());
    }
    private void updateCategoryFields(Category category, CategoryUpdateRequest request) {
        applyNormalizedFields(category, request.getName(), request.getDescription());
    }

    private void applyNormalizedFields(Category category, String name, String description) {
        String normalizedName = normalizeName(name);
        String normalizedDescription = normalizeNullable(description);
        category.setName(normalizedName);
        category.setDescription(normalizedDescription);
    }

    private void validateNameUniqueness(String name, Long currentId) {
        boolean exists = currentId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) {
            throw new CategoryAlreadyExistsException(name);
        }
    }

    private String normalizeName(String value) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new CategoryValidationException("Category name must not be blank");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
