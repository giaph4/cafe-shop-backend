package com.giapho.coffee_shop_backend.exception.category;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown khi không tìm thấy danh mục theo mã định danh.
 */
public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(Long categoryId) {
        super(HttpStatus.NOT_FOUND, String.format("Category not found with id: %d", categoryId));
    }
}
