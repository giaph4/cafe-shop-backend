package com.giapho.coffee_shop_backend.service.purchaseorder;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderResponseDTO;
import com.giapho.coffee_shop_backend.mapper.PurchaseOrderMapper;
import com.giapho.coffee_shop_backend.service.purchaseorder.helper.PurchaseOrderAssembler;
import com.giapho.coffee_shop_backend.service.purchaseorder.helper.PurchaseOrderSpecificationBuilder;
import com.giapho.coffee_shop_backend.service.purchaseorder.helper.PurchaseOrderValidator;
import com.giapho.coffee_shop_backend.service.impl.PurchaseOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private PurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private PurchaseOrderValidator purchaseOrderValidator;
    @Mock
    private PurchaseOrderSpecificationBuilder specificationBuilder;
    @Mock
    private PurchaseOrderAssembler purchaseOrderAssembler;

    @InjectMocks
    private PurchaseOrderServiceImpl purchaseOrderService;

    private PurchaseOrderRequestDTO baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = new PurchaseOrderRequestDTO();
        baseRequest.setSupplierId(10L);
        baseRequest.setItems(List.of()); // Items handled by assembler mock.
    }

    @Test
    void createPurchaseOrder_shouldAssembleAndPersist() {
        User currentUser = buildUser("manager");
        Supplier supplier = buildSupplier(10L, "ACME");
        PurchaseOrder purchaseOrder = buildPurchaseOrder( null, "PENDING");
        PurchaseOrder savedOrder = buildPurchaseOrder(5L, "PENDING");
        PurchaseOrderResponseDTO response = buildResponse(5L, "PENDING");

        when(purchaseOrderValidator.requireCurrentUser()).thenReturn(currentUser);
        when(purchaseOrderValidator.requireSupplier(10L)).thenReturn(supplier);
        when(purchaseOrderAssembler.buildPurchaseOrder(eq(supplier), eq(currentUser), eq(baseRequest), any()))
                .thenReturn(purchaseOrder);
        when(purchaseOrderRepository.save(purchaseOrder)).thenReturn(savedOrder);
        when(purchaseOrderMapper.entityToResponse(savedOrder)).thenReturn(response);

        PurchaseOrderResponseDTO result = purchaseOrderService.createPurchaseOrder(baseRequest);

        assertThat(result).isEqualTo(response);
        verify(purchaseOrderRepository).save(purchaseOrder);
    }

    @Test
    void getAllPurchaseOrders_shouldReturnPagedResponses() {
        Specification<PurchaseOrder> spec = (root, query, builder) -> builder.conjunction();
        PageRequest pageRequest = PageRequest.of(0, 5);
        PurchaseOrder purchaseOrder = buildPurchaseOrder(1L, "PENDING");
        PurchaseOrderResponseDTO response = buildResponse(1L, "PENDING");
        Page<PurchaseOrder> page = new PageImpl<>(List.of(purchaseOrder), pageRequest, 1);

        when(specificationBuilder.build("PENDING", 2L, LocalDate.now().minusDays(1), LocalDate.now()))
                .thenReturn(spec);
        when(purchaseOrderRepository.findAll(spec, pageRequest)).thenReturn(page);
        when(purchaseOrderMapper.entityToResponse(purchaseOrder)).thenReturn(response);

        Page<PurchaseOrderResponseDTO> result = purchaseOrderService.getAllPurchaseOrders(
                "PENDING", 2L, LocalDate.now().minusDays(1), LocalDate.now(), pageRequest
        );

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getPurchaseOrderById_shouldReturnMappedResponse() {
        PurchaseOrder purchaseOrder = buildPurchaseOrder(3L, "PENDING");
        PurchaseOrderResponseDTO response = buildResponse(3L, "PENDING");

        when(purchaseOrderValidator.requirePurchaseOrder(3L)).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.entityToResponse(purchaseOrder)).thenReturn(response);

        PurchaseOrderResponseDTO result = purchaseOrderService.getPurchaseOrderById(3L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void markPurchaseOrderAsCompleted_shouldUpdateStatusAndStock() {
        PurchaseOrder purchaseOrder = buildPurchaseOrder(4L, "PENDING");
        Ingredient ingredient = buildIngredient(1L, BigDecimal.TEN);
        PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                .purchaseOrder(purchaseOrder)
                .ingredient(ingredient)
                .quantity(BigDecimal.valueOf(5))
                .unitPrice(BigDecimal.ONE)
                .build();
        purchaseOrder.setPurchaseOrderDetails(Set.of(detail));
        PurchaseOrderResponseDTO response = buildResponse(4L, "COMPLETED");

        when(purchaseOrderValidator.requirePurchaseOrder(4L)).thenReturn(purchaseOrder);
        doNothing().when(purchaseOrderValidator).ensureCompletable(purchaseOrder);
        when(purchaseOrderRepository.save(purchaseOrder)).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.entityToResponse(purchaseOrder)).thenReturn(response);

        PurchaseOrderResponseDTO result = purchaseOrderService.markPurchaseOrderAsCompleted(4L);

        assertThat(result).isEqualTo(response);
        assertThat(ingredient.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(15));
        verify(purchaseOrderValidator).ensureCompletable(purchaseOrder);
    }

    @Test
    void cancelPurchaseOrder_shouldUpdateStatus() {
        PurchaseOrder purchaseOrder = buildPurchaseOrder(6L, "PENDING");
        PurchaseOrderResponseDTO response = buildResponse(6L, "CANCELLED");

        when(purchaseOrderValidator.requirePurchaseOrder(6L)).thenReturn(purchaseOrder);
        doNothing().when(purchaseOrderValidator).ensureCancelable(purchaseOrder);
        when(purchaseOrderRepository.save(purchaseOrder)).thenReturn(purchaseOrder);
        when(purchaseOrderMapper.entityToResponse(purchaseOrder)).thenReturn(response);

        PurchaseOrderResponseDTO result = purchaseOrderService.cancelPurchaseOrder(6L);

        assertThat(result).isEqualTo(response);
        assertThat(purchaseOrder.getStatus()).isEqualTo("CANCELLED");
        verify(purchaseOrderValidator).ensureCancelable(purchaseOrder);
    }

    private PurchaseOrder buildPurchaseOrder(Long id, String status) {
        return PurchaseOrder.builder()
                .id(id)
                .status(status)
                .purchaseOrderDetails(Set.of())
                .totalAmount(BigDecimal.ZERO)
                .build();
    }

    private PurchaseOrderResponseDTO buildResponse(Long id, String status) {
        PurchaseOrderResponseDTO dto = new PurchaseOrderResponseDTO();
        dto.setId(id);
        dto.setStatus(status);
        return dto;
    }

    private Supplier buildSupplier(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setName(name);
        return supplier;
    }

    private User buildUser(String username) {
        User user = new User();
        user.setUsername(username);
        return user;
    }

    private Ingredient buildIngredient(Long id, BigDecimal quantity) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setQuantityOnHand(quantity);
        return ingredient;
    }
}
