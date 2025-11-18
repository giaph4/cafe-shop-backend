package com.giapho.coffee_shop_backend.service.purchaseorder.helper;

import com.giapho.coffee_shop_backend.domain.entity.Ingredient;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.exception.ingredient.IngredientNotFoundException;
import com.giapho.coffee_shop_backend.exception.purchaseorder.PurchaseOrderNotFoundException;
import com.giapho.coffee_shop_backend.exception.purchaseorder.PurchaseOrderValidationException;
import com.giapho.coffee_shop_backend.exception.supplier.SupplierNotFoundException;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PurchaseOrderValidator {

    private final PurchaseOrderStatusValidator statusValidator;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional(readOnly = true)
    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || StringUtils.isBlank(authentication.getName())) {
            throw new PurchaseOrderValidationException("Current user context is missing");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(authentication.getName()));
    }

    @Transactional(readOnly = true)
    public Supplier requireSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    @Transactional(readOnly = true)
    public Ingredient requireIngredient(Long ingredientId) {
        return ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));
    }

    @Transactional(readOnly = true)
    public PurchaseOrder requirePurchaseOrder(Long purchaseOrderId) {
        return statusValidator.requirePurchaseOrder(purchaseOrderId);
    }

    public void ensureCompletable(PurchaseOrder purchaseOrder) {
        statusValidator.ensurePending(purchaseOrder, "complete");
    }

    public void ensureCancelable(PurchaseOrder purchaseOrder) {
        statusValidator.ensurePending(purchaseOrder, "cancel");
    }
}
