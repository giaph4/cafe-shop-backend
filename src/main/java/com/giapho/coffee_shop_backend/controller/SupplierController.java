package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.SupplierDTO;
import com.giapho.coffee_shop_backend.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * API Lấy danh sách tất cả nhà cung cấp
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<SupplierDTO>> getAllSuppliers() {
        List<SupplierDTO> suppliers = supplierService.getAllSuppliers();
        return ResponseEntity.ok(suppliers);
    }

    /**
     * API Lấy chi tiết nhà cung cấp theo ID
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id) {
        SupplierDTO supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(supplier);
    }

    /**
     * API Tạo nhà cung cấp mới
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SupplierDTO> createSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        SupplierDTO createdSupplier = supplierService.createSupplier(supplierDTO);
        return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    }

    /**
     * API Cập nhật thông tin nhà cung cấp
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<SupplierDTO> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDTO supplierDTO
    ) {
        SupplierDTO updatedSupplier = supplierService.updateSupplier(id, supplierDTO);
        return ResponseEntity.ok(updatedSupplier);
    }

    /**
     * API Xoá nhà cung cấp
     * Chỉ ADMIN mới có quyền xoá (ví dụ phân quyền chặt hơn).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Chỉ Admin được xóa
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}