package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Category;
import com.giapho.coffee_shop_backend.domain.repository.CategoryRepository;
import com.giapho.coffee_shop_backend.dto.category.CategoryCreateRequest;
import com.giapho.coffee_shop_backend.dto.category.CategoryResponse;
import com.giapho.coffee_shop_backend.dto.category.CategoryUpdateRequest;
import com.giapho.coffee_shop_backend.exception.category.CategoryAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.category.CategoryNotFoundException;
import com.giapho.coffee_shop_backend.exception.category.CategoryValidationException;
import com.giapho.coffee_shop_backend.mapper.CategoryMapper;
import com.giapho.coffee_shop_backend.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    private static final Sort EXPECTED_SORT = Sort.by(Sort.Direction.ASC, "name");

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setupMapperDefaults() {
        lenient().when(categoryMapper.toResponseList(any())).thenReturn(List.of());
    }

    @Test
    void createCategory_shouldNormalizeAndPersist() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("  Coffee  ")
                .description("  Hot drinks  ")
                .build();

        Category entity = new Category();
        Category savedEntity = new Category();
        savedEntity.setId(10L);
        savedEntity.setName("Coffee");
        savedEntity.setDescription("Hot drinks");

        CategoryResponse response = CategoryResponse.builder()
                .id(10L)
                .name("Coffee")
                .description("Hot drinks")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Coffee")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(savedEntity);
        when(categoryMapper.toResponse(savedEntity)).thenReturn(response);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        Category persisted = categoryCaptor.getValue();
        assertThat(persisted.getName()).isEqualTo("Coffee");
        assertThat(persisted.getDescription()).isEqualTo("Hot drinks");
    }

    @Test
    void createCategory_shouldRejectDuplicateName() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("Coffee")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Coffee")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_shouldRejectBlankName() {
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name("   ")
                .build();

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CategoryValidationException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getAllCategories_shouldReturnResponsesSortedByName() {
        Category categoryA = new Category();
        Category categoryB = new Category();
        List<Category> entities = List.of(categoryA, categoryB);

        CategoryResponse responseA = CategoryResponse.builder().id(1L).name("A").build();
        CategoryResponse responseB = CategoryResponse.builder().id(2L).name("B").build();
        List<CategoryResponse> responses = List.of(responseA, responseB);

        when(categoryRepository.findAll(EXPECTED_SORT)).thenReturn(entities);
        when(categoryMapper.toResponseList(entities)).thenReturn(responses);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).containsExactly(responseA, responseB);
        verify(categoryRepository).findAll(EXPECTED_SORT);
    }

    @Test
    void updateCategory_shouldApplyChanges() {
        Category existing = new Category();
        existing.setId(5L);
        existing.setName("Old");

        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("New Name  ")
                .description("  Desc ")
                .build();

        Category saved = new Category();
        saved.setId(5L);
        saved.setName("New Name");
        saved.setDescription("Desc");

        CategoryResponse response = CategoryResponse.builder()
                .id(5L)
                .name("New Name")
                .description("Desc")
                .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("New Name", 5L)).thenReturn(false);
        when(categoryRepository.save(existing)).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(response);

        CategoryResponse result = categoryService.updateCategory(5L, request);

        assertThat(result).isEqualTo(response);
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getDescription()).isEqualTo("Desc");
    }

    @Test
    void updateCategory_shouldRejectDuplicateName() {
        Category existing = new Category();
        existing.setId(7L);

        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("Duplicate")
                .build();

        when(categoryRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Duplicate", 7L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(7L, request))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_shouldThrowWhenMissing() {
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("Any")
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999L, request))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteCategory_shouldRemoveEntity() {
        Category existing = new Category();
        existing.setId(3L);

        when(categoryRepository.findById(3L)).thenReturn(Optional.of(existing));

        categoryService.deleteCategory(3L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteCategory_shouldThrowWhenNotFound() {
        when(categoryRepository.findById(1000L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(1000L))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }
}
