package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.category.CategoryCreateRequest;
import com.giapho.coffee_shop_backend.dto.category.CategoryResponse;
import com.giapho.coffee_shop_backend.dto.category.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    List<CategoryResponse> getAllCategories();

    CategoryResponse updateCategory(Long id, CategoryUpdateRequest request);

    void deleteCategory(Long id);
}
