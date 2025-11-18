package com.giapho.coffee_shop_backend.service.supplier.helper;

import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.exception.supplier.SupplierConflictException;
import com.giapho.coffee_shop_backend.exception.supplier.SupplierNotFoundException;
import com.giapho.coffee_shop_backend.exception.supplier.SupplierValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SupplierValidator {

    private static final Logger log = LoggerFactory.getLogger(SupplierValidator.class);

    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public Supplier requireExistingSupplier(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    public String normalizeName(String rawName) {
        if (StringUtils.isBlank(rawName)) {
            throw new SupplierValidationException("Supplier name must not be blank");
        }
        String normalized = StringUtils.normalizeSpace(rawName);
        log.debug("Normalized supplier name '{}' -> '{}'", rawName, normalized);
        return normalized;
    }

    public String normalizePhone(String rawPhone) {
        if (StringUtils.isBlank(rawPhone)) {
            throw new SupplierValidationException("Supplier phone must not be blank");
        }
        return StringUtils.deleteWhitespace(rawPhone);
    }

    public void ensureNameUnique(String normalizedName, Long currentId) {
        Optional<Supplier> existing = supplierRepository.findByName(normalizedName);
        existing.filter(supplier -> currentId != null && supplier.getId().equals(currentId)).ifPresent(supplier -> log.debug("Supplier name unchanged for id {}", currentId));
        if (existing.isPresent() && (currentId == null || !existing.get().getId().equals(currentId))) {
            throw new SupplierConflictException("Supplier name already exists: " + normalizedName);
        }
    }

    public void ensurePhoneUnique(String normalizedPhone, Long currentId) {
        Optional<Supplier> existing = supplierRepository.findByPhone(normalizedPhone);
        existing.filter(supplier -> currentId != null && supplier.getId().equals(currentId)).ifPresent(supplier -> log.debug("Supplier phone unchanged for id {}", currentId));
        if (existing.isPresent() && (currentId == null || !existing.get().getId().equals(currentId))) {
            throw new SupplierConflictException("Supplier phone already exists: " + normalizedPhone);
        }
    }

}
