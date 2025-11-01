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
    private final VoucherService voucherService; // <-- Đã được inject

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
        // Chuyển LocalDate thành LocalDateTime để so sánh với trường created_at trong database
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Page<Order> orderPage = orderRepository.findByCreatedAtBetween(startDateTime, endDateTime, pageable);
        return orderPage.map(orderMapper::entityToResponse);
    }


    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        // Sử dụng findById và fetch EAGER details để đảm bảo có đủ thông tin
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return orderMapper.entityToResponse(order);
    }

    /**
     * Lấy đơn hàng đang PENDING của một bàn
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getPendingOrderByTable(Long tableId) {
        // Kiểm tra bàn tồn tại trước
        if (!cafeTableRepository.existsById(tableId)) {
            throw new EntityNotFoundException("Table not found with id: " + tableId);
        }
        Order order = orderRepository.findPendingOrderByTableId(tableId)
                .orElseThrow(() -> new EntityNotFoundException("No pending order found for table id: " + tableId));
        return orderMapper.entityToResponse(order);
    }

    /**
     * Tạo một Order mới
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO request) {
        // --- Lấy thông tin User và Customer ---
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + request.getCustomerId()));
        }

        // --- Kiểm tra Bàn ---
        CafeTable table = null;
        if (request.getTableId() != null) {
            table = cafeTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + request.getTableId()));
            validateTableForNewOrder(table); // Gọi hàm kiểm tra bàn
        }

        // --- Tạo Order ban đầu ---
        Order newOrder = Order.builder()
                .user(currentUser)
                .cafeTable(table)
                .customer(customer)
                .type(request.getType())
                .status("PENDING")
                .voucherCode(request.getVoucherCode()) // Gán voucher code
                .subTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .orderDetails(new HashSet<>())
                .build();

        // --- Xử lý Items và Tạo OrderDetails ---
        Set<OrderDetail> details = processOrderItems(request.getItems(), newOrder);
        newOrder.setOrderDetails(details);

        // --- Tính toán lại tổng tiền (bao gồm voucher) ---
        recalculateOrderTotals(newOrder); // SỬA LỖI: Hàm này giờ đã dùng VoucherService

        // --- Lưu Order ---
        Order savedOrder = orderRepository.save(newOrder);

        // --- Cập nhật trạng thái bàn ---
        updateTableStatusOnOrderCreate(savedOrder.getCafeTable());

        // --- Trả về DTO (Fetch lại để đảm bảo EAGER loading) ---
        return fetchAndMapOrder(savedOrder.getId(), "Failed to fetch newly created order");
    }

    /**
     * Thêm một món mới vào Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO addItemToOrder(Long orderId, OrderDetailRequestDTO itemDTO) {
        Order order = findPendingOrderById(orderId); // Tìm và kiểm tra PENDING
        Product product = findAvailableProductById(itemDTO.getProductId()); // Tìm và kiểm tra Available

        Optional<OrderDetail> existingDetailOpt = order.getOrderDetails().stream()
                .filter(detail -> detail.getProduct().getId().equals(itemDTO.getProductId()))
                .findFirst();

        if (existingDetailOpt.isPresent()) {
            // Tăng số lượng
            OrderDetail existingDetail = existingDetailOpt.get();
            existingDetail.setQuantity(existingDetail.getQuantity() + itemDTO.getQuantity());
            // Cập nhật notes nếu cần
            if (itemDTO.getNotes() != null) { // Chỉ cập nhật nếu note mới được cung cấp
                existingDetail.setNotes(itemDTO.getNotes());
            }
        } else {
            // Tạo OrderDetail mới
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
                .filter(detail -> detail.getId() != null && detail.getId().equals(orderDetailId)) // Thêm kiểm tra null cho ID
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("OrderDetail not found with id: " + orderDetailId + " in Order id: " + orderId));

        detailToUpdate.setQuantity(updateDTO.getQuantity());
        detailToUpdate.setNotes(updateDTO.getNotes());

        recalculateOrderTotals(order); // SỬA LỖI: Hàm này giờ đã dùng VoucherService
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

        order.getOrderDetails().remove(detailToRemove); // OrphanRemoval=true sẽ xóa record

        // Nếu xóa hết món, reset tiền và voucher
        if (order.getOrderDetails().isEmpty()) {
            resetOrderTotalsAndVoucher(order);
        } else {
            recalculateOrderTotals(order); // SỬA LỖI: Hàm này giờ đã dùng VoucherService
        }

        orderRepository.save(order);

        return fetchAndMapOrder(orderId, "Failed to fetch order after removing item");
    }

    /**
     * Thanh toán một Order đang PENDING
     */
    @Transactional
    public OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest) {
        Order order = findPendingOrderById(orderId);

        String paymentMethod = validatePaymentMethod(paymentRequest.getPaymentMethod());

        subtractInventoryForOrder(order);

        // Lưu voucher code trước khi thanh toán
        String appliedVoucherCode = order.getVoucherCode();

        // Cập nhật Order
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        orderRepository.save(order);

        log.info("Order {} paid successfully with payment method: {}", orderId, paymentMethod);

        // *** CẢI TIẾN: Tăng usage count cho voucher ***
        if (appliedVoucherCode != null && !appliedVoucherCode.isEmpty()) {
            try {
                voucherService.incrementUsageCount(appliedVoucherCode);
                log.info("Incremented usage count for voucher: {}", appliedVoucherCode);
            } catch (Exception e) {
                log.error("Failed to increment voucher usage for code: {}", appliedVoucherCode, e);
                // Không throw exception để không ảnh hưởng đến việc thanh toán
            }
        }

        // Cập nhật Bàn
        updateTableStatusOnOrderCompletion(order.getCafeTable());

        // Cộng điểm thưởng
        if (order.getCustomer() != null) {
            addLoyaltyPoints(order);
        }

        return fetchAndMapOrder(orderId, "Failed to fetch paid order");
    }

    @Transactional
    public OrderResponseDTO applyVoucher(Long orderId, String voucherCode) {
        Order order = findPendingOrderById(orderId);

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Voucher code cannot be empty");
        }

        // Kiểm tra voucher với VoucherService
        VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(
                voucherCode.trim().toUpperCase(),
                order.getSubTotal()
        );

        if (!voucherCheck.isValid()) {
            throw new IllegalArgumentException(voucherCheck.getMessage());
        }

        // Apply voucher
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

        // Remove voucher
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
        orderRepository.save(order); // Lưu trạng thái CANCELLED

        // Cập nhật Bàn
        updateTableStatusOnOrderCompletion(order.getCafeTable());

        // (Hoàn kho/điểm nếu cần)

        return fetchAndMapOrder(orderId, "Failed to fetch cancelled order");
    }

    // ==========================================================
    // HÀM HELPER (Private Methods)
    // ==========================================================

    /**
     * Tìm Order theo ID và kiểm tra trạng thái PENDING
     */
    private Order findPendingOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not in PENDING status. Current status: " + order.getStatus());
        }
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
            // Kiểm tra xem có đơn PENDING nào khác không
            orderRepository.findPendingOrderByTableId(table.getId()).ifPresent(existingOrder -> {
                throw new IllegalArgumentException("Table " + table.getName() + " already has a pending order (ID: " + existingOrder.getId() + ")");
            });
            // Chỉ cho phép tạo trên bàn EMPTY (hoặc có thể mở rộng cho SERVING nếu muốn gộp đơn)
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
                if (detail.getPriceAtOrder() != null && detail.getQuantity() > 0) { // Thêm kiểm tra quantity > 0
                    subTotal = subTotal.add(
                            detail.getPriceAtOrder().multiply(BigDecimal.valueOf(detail.getQuantity()))
                    );
                }
            }
        }
        order.setSubTotal(subTotal);

        // --- BẮT ĐẦU KHỐI SỬA LỖI ---
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (order.getVoucherCode() != null && !order.getVoucherCode().isEmpty()) {
            try {
                // SỬA LỖI: Dùng VoucherService thật
                VoucherCheckResponseDTO voucherCheck = voucherService.checkAndCalculateDiscount(
                        order.getVoucherCode(),
                        subTotal
                );

                if (voucherCheck.isValid()) {
                    discountAmount = voucherCheck.getDiscountAmount();
                } else {
                    // Voucher không còn hợp lệ (ví dụ: subTotal thay đổi, không đủ điều kiện)
                    log.warn("Voucher {} is no longer valid for order {}. Removing.", order.getVoucherCode(), order.getId());
                    order.setVoucherCode(null); // Xóa voucher
                }
            } catch (EntityNotFoundException e) {
                // Voucher không tồn tại
                log.warn("Voucher {} not found during recalculation. Removing.", order.getVoucherCode());
                order.setVoucherCode(null); // Xóa voucher
            }
        }
        // --- KẾT THÚC KHỐI SỬA LỖI ---

        // Đảm bảo discount không lớn hơn subTotal
        discountAmount = discountAmount.min(subTotal);
        order.setDiscountAmount(discountAmount);

        // Tính TotalAmount
        BigDecimal totalAmount = subTotal.subtract(discountAmount);
        order.setTotalAmount(totalAmount.max(BigDecimal.ZERO)); // Đảm bảo không âm
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
            // Kiểm tra xem còn đơn PENDING nào khác trên bàn không
            boolean hasOtherPendingOrder = orderRepository.findPendingOrderByTableId(table.getId())
                    .isPresent(); // Không cần filter vì đơn hiện tại đã PAID/CANCELLED

            // Chỉ trả về EMPTY nếu bàn đang SERVING và không còn đơn PENDING nào khác
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

        // Logic tính điểm: 10,000 VND = 1 điểm
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
            if (product == null) continue; // Bỏ qua nếu không có product (dữ liệu lỗi?)
            int orderQuantity = detail.getQuantity();

            List<ProductIngredient> recipe = productIngredientRepository.findByProductId(product.getId());

            if (recipe.isEmpty()) {
                System.out.println("WARN: No recipe found for product ID: " + product.getId() + ", Name: " + product.getName() + ". Skipping stock deduction.");
                continue;
            }

            for (ProductIngredient pi : recipe) {
                Ingredient ingredient = pi.getIngredient();
                if (ingredient == null) continue; // Bỏ qua nếu không có ingredient (dữ liệu lỗi?)

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
                // ingredientRepository.save(currentIngredient); // Không cần thiết
            }
        }
    }

    /**
     * Hàm helper fetch lại Order và map sang DTO
     */
    private OrderResponseDTO fetchAndMapOrder(Long orderId, String errorMessage) {
        Order fetchedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException(errorMessage + " with id: " + orderId));
        return orderMapper.entityToResponse(fetchedOrder);
    }


}