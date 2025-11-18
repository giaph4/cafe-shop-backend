package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderResponseDTO;
import com.giapho.coffee_shop_backend.mapper.PurchaseOrderMapper;
import com.giapho.coffee_shop_backend.service.PurchaseOrderService;
import com.giapho.coffee_shop_backend.service.purchaseorder.helper.PurchaseOrderAssembler;
import com.giapho.coffee_shop_backend.service.purchaseorder.helper.PurchaseOrderSpecificationBuilder;
import com.giapho.coffee_shop_backend.service.purchaseorder.helper.PurchaseOrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderValidator purchaseOrderValidator;
    private final PurchaseOrderSpecificationBuilder specificationBuilder;
    private final PurchaseOrderAssembler purchaseOrderAssembler;

    @Override
    public PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO request) {
        User currentUser = purchaseOrderValidator.requireCurrentUser();
        Supplier supplier = purchaseOrderValidator.requireSupplier(request.getSupplierId());

        PurchaseOrder purchaseOrder = purchaseOrderAssembler.buildPurchaseOrder(
                supplier,
                currentUser,
                request,
                purchaseOrderValidator::requireIngredient
        );

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        log.info("Created purchase order {} for supplier {}", saved.getId(), supplier.getName());
        return purchaseOrderMapper.entityToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDTO> getAllPurchaseOrders(String status,
                                                               Long supplierId,
                                                               LocalDate startDate,
                                                               LocalDate endDate,
                                                               Pageable pageable) {
        return purchaseOrderRepository
                .findAll(specificationBuilder.build(status, supplierId, startDate, endDate), pageable)
                .map(purchaseOrderMapper::entityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponseDTO getPurchaseOrderById(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderValidator.requirePurchaseOrder(id);
        return purchaseOrderMapper.entityToResponse(purchaseOrder);
    }

    @Override
    public PurchaseOrderResponseDTO markPurchaseOrderAsCompleted(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderValidator.requirePurchaseOrder(id);
        purchaseOrderValidator.ensureCompletable(purchaseOrder);

        purchaseOrder.setStatus("COMPLETED");
        updateIngredientStockOnCompletion(purchaseOrder);

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        log.info("Completed purchase order {}", saved.getId());
        return purchaseOrderMapper.entityToResponse(saved);
    }

    @Override
    public PurchaseOrderResponseDTO cancelPurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = purchaseOrderValidator.requirePurchaseOrder(id);
        purchaseOrderValidator.ensureCancelable(purchaseOrder);

        purchaseOrder.setStatus("CANCELLED");
        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        log.info("Cancelled purchase order {}", saved.getId());
        return purchaseOrderMapper.entityToResponse(saved);
    }

    private void updateIngredientStockOnCompletion(PurchaseOrder purchaseOrder) {
        for (PurchaseOrderDetail detail : purchaseOrder.getPurchaseOrderDetails()) {
            Ingredient ingredient = detail.getIngredient();
            BigDecimal currentQuantity = ingredient.getQuantityOnHand();
            BigDecimal receivedQuantity = detail.getQuantity();
            ingredient.setQuantityOnHand(currentQuantity.add(receivedQuantity));
        }
    }
}
