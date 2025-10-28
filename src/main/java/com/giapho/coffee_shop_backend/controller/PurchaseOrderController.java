package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.PurchaseOrderRequestDTO;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderResponseDTO;
import com.giapho.coffee_shop_backend.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * API Tạo phiếu nhập hàng mới
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDTO request
    ) {
        PurchaseOrderResponseDTO createdPO = purchaseOrderService.createPurchaseOrder(request);
        return new ResponseEntity<>(createdPO, HttpStatus.CREATED);
    }

    /**
     * API Lấy danh sách phiếu nhập hàng (có phân trang VÀ LỌC)
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<PurchaseOrderResponseDTO>> getAllPurchaseOrders(
            // --- Thêm các tham số lọc ---
            @RequestParam(required = false) String status, // Lọc theo trạng thái
            @RequestParam(required = false) Long supplierId, // Lọc theo ID nhà cung cấp
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, // Lọc từ ngày
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, // Lọc đến ngày
            // ---------------------------
            @PageableDefault(size = 10, page = 0, sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // Truyền các tham số lọc vào service
        Page<PurchaseOrderResponseDTO> purchaseOrders = purchaseOrderService.getAllPurchaseOrders(
                status, supplierId, startDate, endDate, pageable
        );
        return ResponseEntity.ok(purchaseOrders);
    }

    /**
     * API Lấy chi tiết một phiếu nhập hàng theo ID
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderResponseDTO purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(purchaseOrder);
    }

    /**
     * API Đánh dấu phiếu nhập hàng là HOÀN THÀNH (COMPLETED)
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     * Dùng POST vì nó thay đổi trạng thái và cập nhật tồn kho.
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> markAsCompleted(@PathVariable Long id) {
        PurchaseOrderResponseDTO completedPO = purchaseOrderService.markPurchaseOrderAsCompleted(id);
        return ResponseEntity.ok(completedPO);
    }

    /**
     * API Huỷ một phiếu nhập hàng đang PENDING
     * Chỉ MANAGER hoặc ADMIN mới có quyền.
     * Dùng POST vì nó thay đổi trạng thái.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> cancelPurchaseOrder(@PathVariable Long id) {
        PurchaseOrderResponseDTO cancelledPO = purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.ok(cancelledPO);
    }
}