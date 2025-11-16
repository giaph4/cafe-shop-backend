package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.*;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CafeTableRepository cafeTableRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;
    private final VoucherService voucherService;
    private final PaymentService paymentService;

    /**
     * Lấy danh sách Order (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return orderPage.map(orderMapper::entityToResponse);
    }

    /**
     * Lấy danh sách Order theo khoảng thời gian (có phân trang)
     *
     * @param startDate Ngày bắt đầu (inclusive)
     * @param endDate   Ngày kết thúc (inclusive)
     * @param pageable  Thông tin phân trang
     * @return Trang các OrderResponseDTO
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Page<Order> orderPage = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime, pageable);
        return orderPage.map(orderMapper::entityToResponse);
    }


    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return orderMapper.entityToResponse(order);
    }

    /**
     * Lấy đơn hàng đang PENDING của một bàn
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getPendingOrderByTable(Long tableId) {
        if (!cafeTableRepository.existsById(tableId)) {
            throw new EntityNotFoundException("Table not found with id: " + tableId);
        }
        Order order = orderRepository.findPendingOrderByTableId(tableId)
                .orElseThrow(() -> new EntityNotFoundException("No pending order found for table id: " + tableId));
        return orderMapper.entityToResponse(order);
    }


    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        // Load customer with proper error handling
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
            log.info("Associating customer ID {} with new order", customer.getId());
        } else {
            log.info("No customer associated with new order");
        }

        CafeTable table = null;
        if (request.getTableId() != null) {
            table = cafeTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + request.getTableId()));
            validateTableForNewOrder(table);
        }

        Order newOrder = Order.builder()
                .user(currentUser)
                .cafeTable(table)
                .customer(customer)
                .type(request.getType())
                .status("PENDING")
                .voucherCode(request.getVoucherCode())
                .subTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .orderDetails(new HashSet<>())
                .build();

        Set<OrderDetail> details = processOrderItems(request.getItems(), newOrder);
        newOrder.setOrderDetails(details);

        recalculateOrderTotals(newOrder);

        Order savedOrder = orderRepository.save(newOrder);

        updateTableStatusOnOrderCreate(savedOrder.getCafeTable());

        return orderMapper.entityToResponse(savedOrder);
    }

    /**
     * Thêm một món mới vào Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO addItemToOrder(Long orderId, OrderDetailRequestDTO itemDTO) {
        Order order = findPendingOrderById(orderId);
        Product product = findAvailableProductById(itemDTO.getProductId());

        Optional<OrderDetail> existingDetailOpt = order.getOrderDetails().stream()
                .filter(detail -> detail.getProduct().getId().equals(itemDTO.getProductId()))
                .findFirst();

        if (existingDetailOpt.isPresent()) {
            OrderDetail existingDetail = existingDetailOpt.get();
            existingDetail.setQuantity(existingDetail.getQuantity() + itemDTO.getQuantity());
            if (itemDTO.getNotes() != null) {
                existingDetail.setNotes(itemDTO.getNotes());
            }
        } else {
            OrderDetail newDetail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .priceAtOrder(product.getPrice())
                    .notes(itemDTO.getNotes())
                    .build();
            order.getOrderDetails().add(newDetail);
        }

        recalculateOrderTotals(order);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.entityToResponse(savedOrder);
    }

    /**
     * Cập nhật thông tin một món (OrderDetail) trong Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO updateItemInOrder(Long orderId, Long orderDetailId, OrderDetailUpdateRequestDTO updateDTO) {
        Order order = findPendingOrderById(orderId);

        OrderDetail detailToUpdate = order.getOrderDetails().stream()
                .filter(detail -> detail.getId() != null && detail.getId().equals(orderDetailId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("OrderDetail not found with id: " + orderDetailId + " in Order id: " + orderId));

        detailToUpdate.setQuantity(updateDTO.getQuantity());
        detailToUpdate.setNotes(updateDTO.getNotes());

        recalculateOrderTotals(order);
        Order savedOrder = orderRepository.save(order);

        return orderMapper.entityToResponse(savedOrder);
    }

    /**
     * Xoá một món (OrderDetail) khỏi Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO removeItemFromOrder(Long orderId, Long orderDetailId) {
        Order order = findPendingOrderById(orderId);

        OrderDetail detailToRemove = order.getOrderDetails().stream()
                .filter(detail -> detail.getId() != null && detail.getId().equals(orderDetailId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("OrderDetail not found with id: " + orderDetailId + " in Order id: " + orderId));

        order.getOrderDetails().remove(detailToRemove);

        if (order.getOrderDetails().isEmpty()) {
            resetOrderTotalsAndVoucher(order);
        } else {
            recalculateOrderTotals(order);
        }

        Order savedOrder = orderRepository.save(order);

        return orderMapper.entityToResponse(savedOrder);
    }

    /**
     * Thanh toán một Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest) {
        Order paidOrder = paymentService.processPayment(orderId, paymentRequest);

        updateTableStatusOnOrderCompletion(paidOrder.getCafeTable());

        return orderMapper.entityToResponse(paidOrder);
    }

    @Transactional
    public OrderResponseDTO applyVoucher(Long orderId, String voucherCode) {
        Order order = findPendingOrderById(orderId);

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Voucher code cannot be empty");
        }

        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(
                voucherCode.trim().toUpperCase(),
                order.getSubTotal()
        );

        if (!voucherCheck.isValid()) {
            throw new IllegalArgumentException(voucherCheck.getMessage());
        }

        order.setVoucherCode(voucherCode.trim().toUpperCase());
        order.setDiscountAmount(voucherCheck.getDiscountAmount());
        order.setTotalAmount(order.getSubTotal().subtract(voucherCheck.getDiscountAmount()));

        Order savedOrder = orderRepository.save(order);

        log.info("Applied voucher {} to order {}. Discount: {}",
                voucherCode, orderId, voucherCheck.getDiscountAmount());

        return orderMapper.entityToResponse(savedOrder);
    }

    @Transactional
    public OrderResponseDTO removeVoucher(Long orderId) {
        Order order = findPendingOrderById(orderId);

        if (order.getVoucherCode() == null) {
            throw new IllegalArgumentException("Order does not have any voucher applied");
        }

        String removedVoucher = order.getVoucherCode();

        order.setVoucherCode(null);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(order.getSubTotal());

        Order savedOrder = orderRepository.save(order);

        log.info("Removed voucher {} from order {}", removedVoucher, orderId);

        return orderMapper.entityToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByStatus(String status, Pageable pageable) {
        if (status == null || status.trim().isEmpty()) {
            return getAllOrders(pageable);
        }
        Page<Order> orders = orderRepository.findByStatus(status.trim().toUpperCase(), pageable);
        return orders.map(orderMapper::entityToResponse);
    }

    /**
     * Huỷ một Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = findPendingOrderById(orderId);

        order.setStatus("CANCELLED");
        Order savedOrder = orderRepository.save(order);

        updateTableStatusOnOrderCompletion(order.getCafeTable());

        return orderMapper.entityToResponse(savedOrder);
    }


    /**
     * Tìm Order theo ID và kiểm tra trạng thái PENDING
     */
    private Order findPendingOrderById(Long orderId) {
        return orderRepository.findPendingOrderByIdWithDetails(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId + " or is not in PENDING status."));

    }

    /**
     * Tìm Product theo ID và kiểm tra isAvailable
     */
    private Product findAvailableProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + productId));
        if (!product.isAvailable()) {
            throw new IllegalArgumentException("Product '" + product.getName() + "' is not available");
        }
        return product;
    }

    /**
     * Kiểm tra trạng thái bàn khi tạo đơn mới
     */
    private void validateTableForNewOrder(CafeTable table) {
        if (table.getStatus() != TableStatus.EMPTY) {
            orderRepository.findPendingOrderByTableId(table.getId()).ifPresent(existingOrder -> {
                throw new IllegalArgumentException("Table " + table.getName() + " already has a pending order (ID: " + existingOrder.getId() + ")");
            });
            throw new IllegalArgumentException("Table " + table.getName() + " is currently " + table.getStatus() + " and cannot receive a new order.");
        }
    }

    /**
     * Xử lý danh sách items khi tạo Order
     */
    private Set<OrderDetail> processOrderItems(List<OrderDetailRequestDTO> itemDTOs, Order order) {
        Set<OrderDetail> details = new HashSet<>();
        for (OrderDetailRequestDTO itemDTO : itemDTOs) {
            Product product = findAvailableProductById(itemDTO.getProductId());
            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .priceAtOrder(product.getPrice())
                    .notes(itemDTO.getNotes())
                    .build();
            details.add(detail);
        }
        return details;
    }

    /**
     * Tính toán lại subTotal, discountAmount, totalAmount cho Order
     * SỬA LỖI: Hàm này đã được sửa để sử dụng VoucherService
     */
    private void recalculateOrderTotals(Order order) {
        BigDecimal subTotal = BigDecimal.ZERO;
        if (order.getOrderDetails() != null) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (detail.getPriceAtOrder() != null && detail.getQuantity() > 0) {
                    subTotal = subTotal.add(
                            detail.getPriceAtOrder().multiply(BigDecimal.valueOf(detail.getQuantity()))
                    );
                }
            }
        }
        order.setSubTotal(subTotal);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (order.getVoucherCode() != null && !order.getVoucherCode().isEmpty()) {
            try {
                VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(
                        order.getVoucherCode(),
                        subTotal
                );

                if (voucherCheck.isValid()) {
                    discountAmount = voucherCheck.getDiscountAmount();
                } else {
                    log.warn("Voucher {} is no longer valid for order {}. Removing.", order.getVoucherCode(), order.getId());
                    order.setVoucherCode(null);
                }
            } catch (EntityNotFoundException e) {
                log.warn("Voucher {} not found during recalculation. Removing.", order.getVoucherCode());
                order.setVoucherCode(null);
            }
        }

        discountAmount = discountAmount.min(subTotal);
        order.setDiscountAmount(discountAmount);

        BigDecimal totalAmount = subTotal.subtract(discountAmount);
        order.setTotalAmount(totalAmount.max(BigDecimal.ZERO));
    }


    /**
     * Reset tiền và voucher khi order rỗng
     */
    private void resetOrderTotalsAndVoucher(Order order) {
        order.setSubTotal(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setVoucherCode(null);
    }

    /**
     * Cập nhật trạng thái bàn khi Order được tạo (chỉ nếu bàn EMPTY)
     */
    private void updateTableStatusOnOrderCreate(CafeTable table) {
        if (table != null && table.getStatus() == TableStatus.EMPTY) {
            table.setStatus(TableStatus.SERVING);
            cafeTableRepository.save(table);
        }
    }

    /**
     * Cập nhật trạng thái bàn khi Order hoàn thành (PAID) hoặc bị hủy (CANCELLED)
     */
    private void updateTableStatusOnOrderCompletion(CafeTable table) {
        if (table != null) {
            boolean hasOtherPendingOrder = orderRepository.findPendingOrderByTableId(table.getId())
                    .isPresent();

            if (!hasOtherPendingOrder && table.getStatus() == TableStatus.SERVING) {
                table.setStatus(TableStatus.EMPTY);
                cafeTableRepository.save(table);
            }
        }
    }
}