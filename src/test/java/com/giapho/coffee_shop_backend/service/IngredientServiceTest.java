package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.dto.IngredientRequestDTO;
import com.giapho.coffee_shop_backend.dto.IngredientResponseDTO;
import com.giapho.coffee_shop_backend.dto.InventoryAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.mapper.IngredientMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientRepository ingredientRepository;
    @Mock
    private IngredientMapper ingredientMapper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private IngredientService ingredientService;

    @BeforeEach
    void setupSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("tester");
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createIngredient_shouldPersistAndReturnResponse() {
        IngredientRequestDTO request = new IngredientRequestDTO();
        request.setName("Arabica Beans");

        Ingredient entity = Ingredient.builder()
                .id(1L)
                .name("Arabica Beans")
                .unit("kg")
                .quantityOnHand(BigDecimal.ZERO)
                .build();

        IngredientResponseDTO response = new IngredientResponseDTO();
        response.setId(1L);
        response.setName("Arabica Beans");

        when(ingredientRepository.existsByName("Arabica Beans")).thenReturn(false);
        when(ingredientMapper.toEntity(request)).thenReturn(entity);
        when(ingredientRepository.save(entity)).thenReturn(entity);
        when(ingredientMapper.entityToResponse(entity)).thenReturn(response);

        IngredientResponseDTO result = ingredientService.createIngredient(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(ingredientRepository).save(entity);
    }

    @Test
    void updateIngredient_shouldApplyChanges() {
        IngredientRequestDTO request = new IngredientRequestDTO();
        request.setName("Robusta Beans");
        request.setUnit("kg");

        Ingredient existing = Ingredient.builder()
                .id(2L)
                .name("Old Name")
                .unit("g")
                .build();

        when(ingredientRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(ingredientRepository.existsByName("Robusta Beans")).thenReturn(false);

        Ingredient updated = Ingredient.builder()
                .id(2L)
                .name("Robusta Beans")
                .unit("kg")
                .build();

        when(ingredientRepository.save(existing)).thenReturn(updated);
        IngredientResponseDTO response = new IngredientResponseDTO();
        response.setId(2L);
        response.setName("Robusta Beans");
        when(ingredientMapper.entityToResponse(updated)).thenReturn(response);

        IngredientResponseDTO result = ingredientService.updateIngredientInfo(2L, request);

        verify(ingredientMapper).updateEntityFromDto(request, existing);
        assertThat(result.getName()).isEqualTo("Robusta Beans");
    }

    @Test
    void adjustInventory_shouldUpdateQuantityAndLog() {
        InventoryAdjustmentRequestDTO request = new InventoryAdjustmentRequestDTO();
        request.setIngredientId(3L);
        request.setNewQuantityOnHand(new BigDecimal("75"));
        request.setReason("Audit");

        Ingredient ingredient = Ingredient.builder()
                .id(3L)
                .name("Milk")
                .unit("L")
                .quantityOnHand(new BigDecimal("50"))
                .build();

        when(ingredientRepository.findById(3L)).thenReturn(Optional.of(ingredient));
        when(ingredientRepository.save(any(Ingredient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IngredientResponseDTO response = new IngredientResponseDTO();
        response.setId(3L);
        response.setQuantityOnHand(new BigDecimal("75"));
        when(ingredientMapper.entityToResponse(ingredient)).thenReturn(response);

        IngredientResponseDTO result = ingredientService.adjustInventory(request);

        assertThat(ingredient.getQuantityOnHand()).isEqualByComparingTo("75");
        assertThat(result.getQuantityOnHand()).isEqualByComparingTo("75");
        verify(ingredientRepository).save(ingredient);
        verify(auditLogService).recordAction(eq("INGREDIENT_INVENTORY_ADJUSTED"), eq("INGREDIENT"), eq("3"), eq(true), anyString(), anyString(), isNull());
    }

    @Test
    void adjustInventory_shouldThrowWhenIngredientMissing() {
        InventoryAdjustmentRequestDTO request = new InventoryAdjustmentRequestDTO();
        request.setIngredientId(999L);
        request.setNewQuantityOnHand(BigDecimal.ONE);

        when(ingredientRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ingredientService.adjustInventory(request));
        verify(auditLogService).recordAction(eq("INGREDIENT_INVENTORY_ADJUSTMENT_FAILED"), eq("INGREDIENT"), eq("999"), eq(false), anyString(), isNull(), anyString());
    }
}
