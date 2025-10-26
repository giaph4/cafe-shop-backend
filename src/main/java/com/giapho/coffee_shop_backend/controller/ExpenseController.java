package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.ExpenseDTO;
import com.giapho.coffee_shop_backend.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat; // Import
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate; // Import

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * API Ghi nhận khoản chi mới
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseDTO> createExpense(@Valid @RequestBody ExpenseDTO expenseDTO) {
        ExpenseDTO createdExpense = expenseService.createExpense(expenseDTO);
        return new ResponseEntity<>(createdExpense, HttpStatus.CREATED);
    }

    /**
     * API Lấy danh sách chi phí (phân trang, lọc theo ngày)
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ExpenseDTO>> getAllExpenses(
            // Tham số lọc theo ngày bắt đầu (tùy chọn)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            // Tham số lọc theo ngày kết thúc (tùy chọn)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 15, page = 0, sort = "expenseDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ExpenseDTO> expenses = expenseService.getAllExpenses(startDate, endDate, pageable);
        return ResponseEntity.ok(expenses);
    }

    /**
     * API Lấy chi tiết một khoản chi theo ID
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long id) {
        ExpenseDTO expense = expenseService.getExpenseById(id);
        return ResponseEntity.ok(expense);
    }

    /**
     * API Cập nhật thông tin một khoản chi
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ExpenseDTO> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseDTO expenseDTO
    ) {
        ExpenseDTO updatedExpense = expenseService.updateExpense(id, expenseDTO);
        return ResponseEntity.ok(updatedExpense);
    }

    /**
     * API Xoá một khoản chi
     * Chỉ ADMIN mới có quyền xoá.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }
}