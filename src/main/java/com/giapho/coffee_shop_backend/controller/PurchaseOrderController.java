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

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDTO request
    ) {
        PurchaseOrderResponseDTO createdPO = purchaseOrderService.createPurchaseOrder(request);
        return new ResponseEntity<>(createdPO, HttpStatus.CREATED);
    }

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
        Page<PurchaseOrderResponseDTO> purchaseOrders = purchaseOrderService.getAllPurchaseOrders(
                status, supplierId, startDate, endDate, pageable
        );
        return ResponseEntity.ok(purchaseOrders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderResponseDTO purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(purchaseOrder);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> markAsCompleted(@PathVariable Long id) {
        PurchaseOrderResponseDTO completedPO = purchaseOrderService.markPurchaseOrderAsCompleted(id);
        return ResponseEntity.ok(completedPO);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PurchaseOrderResponseDTO> cancelPurchaseOrder(@PathVariable Long id) {
        PurchaseOrderResponseDTO cancelledPO = purchaseOrderService.cancelPurchaseOrder(id);
        return ResponseEntity.ok(cancelledPO);
    }
}