package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.CustomerDTO;
import com.giapho.coffee_shop_backend.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * API Tạo khách hàng mới
     * Tất cả nhân viên đều có thể thêm khách hàng.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        CustomerDTO createdCustomer = customerService.createCustomer(customerDTO);
        return new ResponseEntity<>(createdCustomer, HttpStatus.CREATED);
    }

    /**
     * API Tìm kiếm và Lấy danh sách khách hàng (phân trang)
     * Tất cả nhân viên đều có thể xem/tìm kiếm.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<CustomerDTO>> searchCustomers(
            @RequestParam(required = false, defaultValue = "") String keyword, // Keyword tìm theo tên hoặc SĐT
            @PageableDefault(size = 15, page = 0, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CustomerDTO> customers = customerService.searchCustomers(keyword, pageable);
        return ResponseEntity.ok(customers);
    }

    /**
     * API Lấy chi tiết khách hàng theo ID
     * Tất cả nhân viên đều có thể xem.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        CustomerDTO customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    /**
     * API Tìm khách hàng theo số điện thoại (tra cứu nhanh)
     * Tất cả nhân viên đều có thể xem.
     */
    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerDTO> getCustomerByPhone(@PathVariable String phone) {
        CustomerDTO customer = customerService.getCustomerByPhone(phone);
        return ResponseEntity.ok(customer);
    }

    /**
     * API Cập nhật thông tin khách hàng
     * Chỉ MANAGER hoặc ADMIN mới có quyền sửa.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDTO customerDTO
    ) {
        CustomerDTO updatedCustomer = customerService.updateCustomer(id, customerDTO);
        return ResponseEntity.ok(updatedCustomer);
    }

    /**
     * API Xoá khách hàng
     * Chỉ ADMIN mới có quyền xoá.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}