package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.*;
import com.giapho.coffee_shop_backend.domain.repository.*;
import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.mapper.OrderDetailMapper;
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
import java.math.RoundingMode;
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
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CafeTableRepository cafeTableRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final OrderMapper orderMapper;
    private final VoucherService voucherService;
    private final CustomerService customerService;

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

        return fetchAndMapOrder(savedOrder.getId(), "Failed to fetch newly created order");
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
        orderRepository.save(order);

        return fetchAndMapOrder(orderId, "Failed to fetch order after adding item");
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
        orderRepository.save(order);

        return fetchAndMapOrder(orderId, "Failed to fetch order after updating item");
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

        orderRepository.save(order);

        return fetchAndMapOrder(orderId, "Failed to fetch order after removing item");
    }

    /**
     * Thanh toán một Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest) {
        // Load order with customer relationship
        Order order = orderRepository.findByIdWithCustomer(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
                
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("Cannot pay order with status: " + order.getStatus());
        }

        String paymentMethod = validatePaymentMethod(paymentRequest.getPaymentMethod());

        // Allow associating customer during payment if not already associated
        if (paymentRequest.getCustomerId() != null && order.getCustomer() == null) {
            Customer customer = customerRepository.findById(paymentRequest.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + paymentRequest.getCustomerId()));
            order.setCustomer(customer);
            log.info("Associated customer ID {} with order {} during payment", customer.getId(), orderId);
        }

        subtractInventoryForOrder(order);

        String appliedVoucherCode = order.getVoucherCode();

        // Cập nhật trạng thái đơn hàng
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        
        // Cập nhật điểm tích lũy cho khách hàng nếu có
        log.info("Processing loyalty points for order {}. Customer: {}, Total Amount: {}", 
                orderId, 
                order.getCustomer() != null ? order.getCustomer().getId() : "null", 
                order.getTotalAmount());
                
        if (order.getCustomer() == null) {
            log.warn("No customer associated with order {}. Cannot add loyalty points.", orderId);
        } else if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid total amount {} for order {}. Cannot add loyalty points.", order.getTotalAmount(), orderId);
        } else {
            try {
                log.info("Attempting to add loyalty points for customer {} with amount {}", 
                        order.getCustomer().getId(), order.getTotalAmount());
                customerService.updateLoyaltyPoints(order.getCustomer().getId(), order.getTotalAmount());
                log.info("Successfully updated loyalty points for customer {}", order.getCustomer().getId());
            } catch (Exception e) {
                log.error("Failed to update loyalty points for customer: {}", order.getCustomer().getId(), e);
            }
        }
        
        order = orderRepository.save(order);
        log.info("Order {} paid successfully with payment method: {}", orderId, paymentMethod);

        updateTableStatusOnOrderCompletion(order.getCafeTable());

        return fetchAndMapOrder(orderId, "Failed to fetch paid order");
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

        orderRepository.save(order);

        log.info("Applied voucher {} to order {}. Discount: {}",
                voucherCode, orderId, voucherCheck.getDiscountAmount());

        return fetchAndMapOrder(orderId, "Failed to fetch order after applying voucher");
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

        orderRepository.save(order);

        log.info("Removed voucher {} from order {}", removedVoucher, orderId);

        return fetchAndMapOrder(orderId, "Failed to fetch order after removing voucher");
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
        orderRepository.save(order);

        updateTableStatusOnOrderCompletion(order.getCafeTable());


        return fetchAndMapOrder(orderId, "Failed to fetch cancelled order");
    }


    /**
     * Tìm Order theo ID và kiểm tra trạng thái PENDING
     */
    private Order findPendingOrderById(Long orderId) {
        Order order = orderRepository.findPendingOrderByIdWithDetails(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId + " or is not in PENDING status."));

        return order;
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
        if (!"EMPTY".equals(table.getStatus())) {
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
     * SỬA LỖI: Xóa hàm `calculateDiscount` hard-coded
     * (Hàm private calculateDiscount(String voucherCode, BigDecimal subTotal) đã bị xóa)
     */

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
        if (table != null && "EMPTY".equals(table.getStatus())) {
            table.setStatus("SERVING");
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

            if (!hasOtherPendingOrder && "SERVING".equals(table.getStatus())) {
                table.setStatus("EMPTY");
                cafeTableRepository.save(table);
            }
        }
    }

    /**
     * Chuẩn hóa và kiểm tra paymentMethod
     */
    private String validatePaymentMethod(String paymentMethodInput) {
        if (paymentMethodInput == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        String paymentMethod = paymentMethodInput.toUpperCase();
        if (!paymentMethod.equals("CASH") && !paymentMethod.equals("TRANSFER") && !paymentMethod.equals("CARD")) {
            throw new IllegalArgumentException("Invalid payment method. Supported methods: CASH, TRANSFER, CARD");
        }
        return paymentMethod;
    }

    /**
     * Hàm helper cộng điểm (đã có)
     */
    private void addLoyaltyPoints(Order order) {
        if (order.getCustomer() == null || order.getTotalAmount() == null ||
                order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int pointsToAdd = order.getTotalAmount()
                .divide(BigDecimal.valueOf(10000), 0, RoundingMode.DOWN)
                .intValue();

        if (pointsToAdd > 0) {
            Customer currentCustomer = customerRepository.findById(order.getCustomer().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Customer disappeared during point calculation"));

            int oldPoints = currentCustomer.getLoyaltyPoints();
            currentCustomer.setLoyaltyPoints(oldPoints + pointsToAdd);

            log.info("Added {} points to customer {} (ID: {}). Old: {}, New: {}",
                    pointsToAdd, currentCustomer.getPhone(), currentCustomer.getId(),
                    oldPoints, currentCustomer.getLoyaltyPoints());
        }
    }

    /**
     * Hàm helper trừ kho (đã có)
     */
    private void subtractInventoryForOrder(Order order) {
        if (order.getOrderDetails() == null) {
            return;
        }

        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            if (product == null) continue;
            int orderQuantity = detail.getQuantity();

            List<ProductIngredient> recipe = productIngredientRepository.findByProductId(product.getId());

            if (recipe.isEmpty()) {
                System.out.println("WARN: No recipe found for product ID: " + product.getId() + ", Name: " + product.getName() + ". Skipping stock deduction.");
                continue;
            }

            for (ProductIngredient pi : recipe) {
                Ingredient ingredient = pi.getIngredient();
                if (ingredient == null) continue;

                BigDecimal quantityNeededPerProduct = pi.getQuantityNeeded();
                BigDecimal totalQuantityToSubtract = quantityNeededPerProduct.multiply(BigDecimal.valueOf(orderQuantity));

                Ingredient currentIngredient = ingredientRepository.findById(ingredient.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Ingredient not found during stock deduction: ID " + ingredient.getId()));

                BigDecimal currentStock = currentIngredient.getQuantityOnHand();

                if (currentStock.compareTo(totalQuantityToSubtract) < 0) {
                    throw new IllegalArgumentException("Not enough stock for ingredient: " + currentIngredient.getName()
                            + ". Required: " + totalQuantityToSubtract + ", Available: " + currentStock);
                }

                currentIngredient.setQuantityOnHand(currentStock.subtract(totalQuantityToSubtract));
            }
        }
    }

    private OrderResponseDTO fetchAndMapOrder(Long orderId, String errorMessage) {
        Order fetchedOrder = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new EntityNotFoundException(errorMessage + " with id: " + orderId));
        return orderMapper.entityToResponse(fetchedOrder);
    }
}