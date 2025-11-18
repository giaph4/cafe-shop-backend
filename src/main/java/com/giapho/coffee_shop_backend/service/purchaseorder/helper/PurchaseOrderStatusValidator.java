package com.giapho.coffee_shop_backend.service.purchaseorder.helper;

import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.exception.purchaseorder.PurchaseOrderNotFoundException;
import com.giapho.coffee_shop_backend.exception.purchaseorder.PurchaseOrderStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderStatusValidator {

    private static final String PENDING = "PENDING";

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional(readOnly = true)
    public PurchaseOrder requirePurchaseOrder(Long purchaseOrderId) {
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(purchaseOrderId));
    }

    public void ensurePending(PurchaseOrder purchaseOrder, String action) {
        if (!PENDING.equals(purchaseOrder.getStatus())) {
            throw new PurchaseOrderStatusException(action, purchaseOrder.getStatus());
        }
    }
}
