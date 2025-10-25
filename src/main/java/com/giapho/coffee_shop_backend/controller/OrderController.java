package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.service.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * API Tạo đơn hàng mới
     * Tất cả nhân viên đều có quyền.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderCreateRequestDTO request) {
        OrderResponseDTO createdOrder = orderService.createOrder(request);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    /**
     * API Lấy tất cả đơn hàng (phân trang)
     * Chỉ MANAGER hoặc ADMIN mới có quyền xem lịch sử tất cả đơn.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderResponseDTO> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * API Lấy chi tiết một đơn hàng theo ID
     * Tất cả nhân viên đều có quyền xem (ví dụ: để in lại bill).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        OrderResponseDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    /**
     * API Lấy đơn hàng đang PENDING (chưa thanh toán) của một bàn cụ thể
     * Tất cả nhân viên đều có quyền.
     */
    @GetMapping("/table/{tableId}/pending")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> getPendingOrderByTable(@PathVariable Long tableId) {
        OrderResponseDTO order = orderService.getPendingOrderByTable(tableId);
        return ResponseEntity.ok(order);
    }

    /**
     * API Thêm món vào một Order đang PENDING
     * Tất cả nhân viên đều có quyền.
     */
    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> addItemToOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderDetailRequestDTO itemDTO
    ) {
        OrderResponseDTO updatedOrder = orderService.addItemToOrder(orderId, itemDTO);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * API Xoá món khỏi một Order đang PENDING
     * Tất cả nhân viên đều có quyền.
     */
    @DeleteMapping("/{orderId}/items/{orderDetailId}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> removeItemFromOrder(
            @PathVariable Long orderId,
            @PathVariable Long orderDetailId
    ) {
        OrderResponseDTO updateOrder = orderService.removeItemFromOrder(orderId, orderDetailId);
        return ResponseEntity.ok(updateOrder);
    }

    @PutMapping("/{orderId}/items/{orderDetailId}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> updateItemInOrder(
            @PathVariable Long orderId,
            @PathVariable Long orderDetailId,
            @Valid @RequestBody OrderDetailUpdateRequestDTO updateDTO
    ) {
        OrderResponseDTO updatedOrder = orderService.updateItemInOrder(orderId, orderDetailId, updateDTO);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * API Thanh toán một Order đang PENDING
     * Tất cả nhân viên đều có quyền.
     */
    @PostMapping("/{orderId}/payment")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> payOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequestDTO paymentRequest
    ) {
        OrderResponseDTO updatedOrder = orderService.payOrder(orderId, paymentRequest);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * API Huỷ một Order đang PENDING
     * Chỉ MANAGER hoặc ADMIN mới có quyền huỷ đơn.
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable Long orderId) {
        OrderResponseDTO cancelledOrder = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(cancelledOrder);
    }
}