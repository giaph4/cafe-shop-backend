package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.dto.SupplierDTO;
import com.giapho.coffee_shop_backend.mapper.SupplierMapper;
import com.giapho.coffee_shop_backend.service.SupplierService;
import com.giapho.coffee_shop_backend.service.supplier.helper.SupplierValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final SupplierValidator supplierValidator;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDTO> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();
        log.debug("Fetched {} suppliers", suppliers.size());
        return supplierMapper.toDtoList(suppliers);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierValidator.requireExistingSupplier(id);
        log.debug("Found supplier {}", supplier.getName());
        return supplierMapper.toDto(supplier);
    }

    @Override
    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        String normalizedName = supplierValidator.normalizeName(supplierDTO.getName());
        String normalizedPhone = supplierValidator.normalizePhone(supplierDTO.getPhone());

        supplierValidator.ensureNameUnique(normalizedName, null);
        supplierValidator.ensurePhoneUnique(normalizedPhone, null);

        Supplier entity = supplierMapper.toEntity(supplierDTO);
        entity.setName(normalizedName);
        entity.setPhone(normalizedPhone);

        Supplier saved = supplierRepository.save(entity);
        log.info("Created supplier {} with id {}", saved.getName(), saved.getId());
        return supplierMapper.toDto(saved);
    }

    @Override
    public SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO) {
        Supplier existing = supplierValidator.requireExistingSupplier(id);

        String normalizedName = supplierValidator.normalizeName(supplierDTO.getName());
        String normalizedPhone = supplierValidator.normalizePhone(supplierDTO.getPhone());

        if (!normalizedName.equals(existing.getName())) {
            supplierValidator.ensureNameUnique(normalizedName, id);
        }

        if (!normalizedPhone.equals(existing.getPhone())) {
            supplierValidator.ensurePhoneUnique(normalizedPhone, id);
        }

        supplierMapper.updateEntityFromDto(supplierDTO, existing);
        existing.setName(normalizedName);
        existing.setPhone(normalizedPhone);

        Supplier updated = supplierRepository.save(existing);
        log.info("Updated supplier {} (id {})", updated.getName(), updated.getId());
        return supplierMapper.toDto(updated);
    }

    @Override
    public void deleteSupplier(Long id) {
        Supplier existing = supplierValidator.requireExistingSupplier(id);
        supplierRepository.delete(existing);
        log.info("Deleted supplier {} (id {})", existing.getName(), existing.getId());
    }
}
