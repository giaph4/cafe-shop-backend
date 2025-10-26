package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.dto.SupplierDTO;
import com.giapho.coffee_shop_backend.mapper.SupplierMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    /**
     * Lấy danh sách tất cả nhà cung cấp (không phân trang)
     * (Có thể thêm phân trang nếu danh sách quá lớn)
     */
    @Transactional(readOnly = true)
    public List<SupplierDTO> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepository.findAll();
        return supplierMapper.toDtoList(suppliers);
    }

    /**
     * Lấy chi tiết một nhà cung cấp
     */
    @Transactional(readOnly = true)
    public SupplierDTO getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return supplierMapper.toDto(supplier);
    }

    /**
     * Tạo mới một nhà cung cấp
     */
    @Transactional(readOnly = true)
    public SupplierDTO createSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));
        return supplierMapper.toDto(supplier);
    }

    @Transactional
    public SupplierDTO createSupplier(SupplierDTO supplierDTO) {
        if (supplierRepository.existsByName(supplierDTO.getName())) {
            throw new IllegalArgumentException("Supplier name already exists: " + supplierDTO.getName());
        }
        if (supplierRepository.existsByPhone(supplierDTO.getPhone())) {
            throw new IllegalArgumentException("Supplier phone number already exists: " + supplierDTO.getPhone());
        }

        Supplier newSupplier = supplierMapper.toEntity(supplierDTO);
        Supplier savedSupplier = supplierRepository.save(newSupplier);
        return supplierMapper.toDto(savedSupplier);
    }

    /**
     * Cập nhật thông tin nhà cung cấp
     */
    @Transactional
    public SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO) {
        Supplier existingSupplier = supplierRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Supplier not found with id: " + id));

        // Kiểm tra tên mới (nếu thay đổi)
        if (!existingSupplier.getName().equals(supplierDTO.getName()) &&
                supplierRepository.existsByName(supplierDTO.getName())) {
            throw new IllegalArgumentException("Supplier name already exists: " + supplierDTO.getName());
        }

        // Kiểm tra SĐT mới (nếu thay đổi)
        if (!existingSupplier.getPhone().equals(supplierDTO.getPhone()) &&
                supplierRepository.existsByPhone(supplierDTO.getPhone())) {
            throw new IllegalArgumentException("Supplier phone number already exists: " + supplierDTO.getPhone());
        }

        supplierMapper.updateEntityFromDto(supplierDTO, existingSupplier);
        Supplier updatedSupplier = supplierRepository.save(existingSupplier);
        return supplierMapper.toDto(updatedSupplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new EntityNotFoundException("Supplier not found with id: " + id);
        }

        supplierRepository.deleteById(id);
    }
}
