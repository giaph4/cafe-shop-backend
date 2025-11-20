package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.service.OrderService;
import com.giapho.coffee_shop_backend.service.order.OrderQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * API to create a new order
 * Only ADMIN or STAFF roles can create a new order
 *
 * @param request the order create request DTO
 * @return the created order response DTO with HTTP status CREATED
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    /**
     * API to create a new order
     * Only ADMIN or STAFF roles can create a new order
     *
     * @param request the order create request DTO
     * @return the created order response DTO with HTTP status CREATED
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderCreateRequestDTO request) {
        OrderResponseDTO order = orderService.createOrder(request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    //Get All Orders
    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<OrderResponseDTO>> getAllOrders(
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderResponseDTO> orders = orderQueryService.getOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long id) {
        log.info("Getting order by ID: {}", id);
        OrderResponseDTO order = orderQueryService.getOrderDetail(id);
        log.info("Retrieved order {}", order);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/table/{tableId}/pending")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> getPendingOrderByTable(@PathVariable Long tableId) {
        OrderResponseDTO order = orderQueryService.getPendingOrderByTable(tableId);
        return ResponseEntity.ok(order);
    }

    /**
     * Thêm một món mới vào Order đang PENDING
     *
     * @param orderId ID của Order
     * @param itemDTO Thông tin của món (OrderDetail) cần thêm
     * @return OrderResponseDTO sau khi thêm món xong
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

    @PostMapping("/{orderId}/payment")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> payOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequestDTO paymentRequest
    ) {
        OrderResponseDTO updatedOrder = orderService.payOrder(orderId, paymentRequest);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/{orderId}/voucher")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> applyVoucher(
            @PathVariable Long orderId,
            @Valid @RequestBody VoucherApplyRequestDTO voucherRequest
    ) {
        OrderResponseDTO updatedOrder = orderService.applyVoucher(orderId, voucherRequest.getVoucherCode());
        return ResponseEntity.ok(updatedOrder);
    }

    @DeleteMapping("/{orderId}/voucher")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> removeVoucher(@PathVariable Long orderId) {
        OrderResponseDTO updatedOrder = orderService.removeVoucher(orderId);
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<OrderResponseDTO>> getOrdersByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderResponseDTO> orders = orderQueryService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<OrderResponseDTO>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderResponseDTO> orders = orderQueryService.getOrdersByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable Long orderId) {
        OrderResponseDTO cancelledOrder = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(cancelledOrder);
    }
}