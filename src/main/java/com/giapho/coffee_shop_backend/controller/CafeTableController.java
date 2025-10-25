package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import com.giapho.coffee_shop_backend.service.CafeTableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class CafeTableController {

    private final CafeTableService cafeTableService;

    /**
     * API Lấy tất cả các bàn (dùng cho sơ đồ bàn)
     * Tất cả nhân viên đều có quyền xem.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<List<CafeTableResponse>> getAllTables() {
        List<CafeTableResponse> tables = cafeTableService.getAllTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * API Lấy chi tiết 1 bàn
     * Tất cả nhân viên đều có quyền xem.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> getTableById(@PathVariable Long id) {
        CafeTableResponse table = cafeTableService.getTableById(id);
        return ResponseEntity.ok(table);
    }

    /**
     * API Tạo bàn mới
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> createTable(@Valid @RequestBody CafeTableRequest request) {
        CafeTableResponse createdTable = cafeTableService.createTable(request);
        return new ResponseEntity<>(createdTable, HttpStatus.CREATED);
    }

    /**
     * API Cập nhật thông tin bàn (tên, sức chứa)
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> updateTableInfo(
            @PathVariable Long id,
            @Valid @RequestBody CafeTableRequest request
    ) {
        CafeTableResponse updatedTable = cafeTableService.updateTableInfo(id, request);
        return ResponseEntity.ok(updatedTable);
    }

    /**
     * API Cập nhật TRẠNG THÁI bàn (nghiệp vụ riêng)
     * Dùng @PatchMapping vì chỉ cập nhật 1 phần.
     * Tất cả nhân viên (STAFF) đều có quyền (để chuyển bàn từ EMPTY -> SERVING).
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CafeTableResponse> updateTableStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusMap // Nhận JSON dạng {"status": "SERVING"}
    ) {
        String status = statusMap.get("status");
        CafeTableResponse updatedTable = cafeTableService.updateTableStatus(id, status);
        return ResponseEntity.ok(updatedTable);
    }

    /**
     * API Xoá bàn
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        cafeTableService.deleteTable(id);
        return ResponseEntity.noContent().build(); // Trả về 204 No Content
    }
}
