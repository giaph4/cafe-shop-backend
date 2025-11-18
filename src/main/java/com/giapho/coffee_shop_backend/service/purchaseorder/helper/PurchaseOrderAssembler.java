package com.giapho.coffee_shop_backend.service.purchaseorder.helper;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderRequestDTO;
import com.giapho.coffee_shop_backend.exception.purchaseorder.PurchaseOrderValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Component
public class PurchaseOrderAssembler {

    private static final String STATUS_PENDING = "PENDING";

    public PurchaseOrder buildPurchaseOrder(Supplier supplier,
                                            User user,
                                            PurchaseOrderRequestDTO request,
                                            Function<Long, Ingredient> ingredientResolver) {
        List<PurchaseOrderDetailRequestDTO> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new PurchaseOrderValidationException("Purchase order must include at least one item");
        }

        Set<PurchaseOrderDetail> details = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .supplier(supplier)
                .user(user)
                .expectedDate(request.getExpectedDate())
                .status(STATUS_PENDING)
                .purchaseOrderDetails(details)
                .totalAmount(BigDecimal.ZERO)
                .build();

        for (PurchaseOrderDetailRequestDTO item : items) {
            Ingredient ingredient = ingredientResolver.apply(item.getIngredientId());
            PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                    .purchaseOrder(purchaseOrder)
                    .ingredient(ingredient)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
            details.add(detail);
            totalAmount = totalAmount.add(item.getQuantity().multiply(item.getUnitPrice()));
        }

        purchaseOrder.setTotalAmount(totalAmount);
        return purchaseOrder;
    }
}
