package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.*;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.mapper.OrderDetailMapper;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CafeTableRepository cafeTableRepository;
    private final UserRepository userRepository; // Để lấy user đang đăng nhập
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;

    /**
     * Lấy danh sách Order (có phân trang)
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return orderPage.map(orderMapper::entityToResponse);
    }

    /**
     * Lấy chi tiết một Order
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return orderMapper.entityToResponse(order);
    }

    /**
     * Tạo một Order mới
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO request) {
        // --- 1. Lấy thông tin User đang đăng nhập ---
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));

        // --- 2. Kiểm tra và lấy thông tin Bàn (nếu có) ---
        CafeTable table;
        if (request.getTableId() != null) {
            table = cafeTableRepository.findById(request.getTableId())
                    .orElseThrow(() -> new EntityNotFoundException("Table not found with id: " + request.getTableId()));

            // Kiểm tra xem bàn có đang trống không
            if (!"EMPTY".equals(table.getStatus())) {
                // Kiểm tra xem có đơn PENDING nào khác cho bàn này không (đề phòng)
                orderRepository.findPendingOrderByTableId(table.getId()).ifPresent(existingOrder -> {
                    throw new IllegalArgumentException("Table " + table.getName() + " already has a pending order (ID: " + existingOrder.getId() + ")");
                });
                // Nếu không có đơn PENDING nhưng status khác EMPTY (ví dụ RESERVED), cũng báo lỗi
                if (!"SERVING".equals(table.getStatus())) {
                    throw new IllegalArgumentException("Table " + table.getName() + " is currently " + table.getStatus());
                }
            }
        } else {
            table = null;
        }

        // --- 3. Tạo đối tượng Order ban đầu (chưa có chi tiết, chưa tính tiền) ---
        Order newOrder = Order.builder()
                .user(currentUser)
                .cafeTable(table)
                .type(request.getType())
                .status("PENDING") // Mặc định là PENDING
                .subTotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .orderDetails(new HashSet<>())
                .build();

        // --- 4. Xử lý từng món hàng trong request ---
        Set<OrderDetail> details = new HashSet<>();
        BigDecimal subTotal = BigDecimal.ZERO;

        for (OrderDetailRequestDTO itemDTO : request.getItems()) {
            // Lấy thông tin sản phẩm
            var product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + itemDTO.getProductId()));

            if (!product.isAvailable()) {
                throw new IllegalArgumentException("Product with id " + itemDTO.getProductId() + " is not available");
            }

            // Tạo OrderDetail
            OrderDetail orderDetail = OrderDetail.builder()
                    .order(newOrder)
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .priceAtOrder(product.getPrice())
                    .notes(itemDTO.getNotes())
                    .build();

            details.add(orderDetail);
            // Cộng dồn vào tổng tiền
            subTotal = subTotal.add(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        // --- 5. Cập nhật lại Order với chi tiết và tổng tiền ---
        newOrder.setOrderDetails(details);
        newOrder.setSubTotal(subTotal);
        newOrder.setTotalAmount(subTotal.subtract(newOrder.getDiscountAmount()));

        Order savedOrder = orderRepository.save(newOrder);

        if (table != null) {
            table.setStatus("SERVING");
            cafeTableRepository.save(table);
        }

        Order fetchedOrder = orderRepository.findById(savedOrder.getId())
                .orElseThrow(() -> new EntityNotFoundException("Failed to fetch newly created order"));

        return orderMapper.entityToResponse(savedOrder);
    }

    /**
     * (Thêm sau) Thêm món vào Order đã tồn tại
     */
    @Transactional
    public OrderResponseDTO addItemToOrder(Long orderId, OrderDetailRequestDTO itemDTO) {
        // 1. Tìm Order đang PENDING
        Order order = orderDetailRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId)).getOrder();

        if (!order.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Order is not pending");
        }

        // 2. Lấy thông tin product
        Product product = productRepository.findById(itemDTO.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + itemDTO.getProductId()));

        if (!product.isAvailable()) {
            throw new IllegalArgumentException("Product with id " + itemDTO.getProductId() + " is not available");
        }

        // 3. Kiểm tra xem món này đã có trong order chưa
        var existingProductOpt = order.getOrderDetails().stream()
                .filter(detail -> detail.getProduct().getId().equals(product.getId()))
                .findFirst();

        // Nếu sản phần tồn tại thì tăng số lượng
        if (existingProductOpt.isPresent()) {
            OrderDetail existingOrderDetail = existingProductOpt.get();
            existingOrderDetail.setQuantity(existingOrderDetail.getQuantity() + itemDTO.getQuantity());
        } else {
            // Nếu chưa có -> Tạo OrderDetail mới
            OrderDetail newDetail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemDTO.getQuantity())
                    .priceAtOrder(product.getPrice())
                    .notes(itemDTO.getNotes())
                    .build();
            order.getOrderDetails().add(newDetail);
        }

        // 4. Tính toán lại tổng tiền
        recalculateOrderTotals(order);

        // 5. Lưu lại Order (Cascade sẽ xử lý OrderDetail)
        Order updatedOrder = orderRepository.save(order);

        // 6. Trả về DTO
        return orderMapper.entityToResponse(updatedOrder);
    }

    public void recalculateOrderTotals(Order order) {
        BigDecimal subTotal = BigDecimal.ZERO;

        if (order.getOrderDetails() != null) {
            for (OrderDetail orderDetail : order.getOrderDetails()) {
                subTotal = subTotal.add(
                        orderDetail.getPriceAtOrder().multiply(BigDecimal.valueOf(orderDetail.getQuantity()))
                );
            }

            order.setSubTotal(subTotal);
        }
        // Tạm thời chưa tính discount
        order.setTotalAmount(subTotal.subtract(order.getDiscountAmount()));
    }

    /**
     * Xoá món khỏi Order đã tồn tại
     */
    @Transactional
    public OrderResponseDTO removeItemFromOrder(Long orderId, Long orderDetailId) {

        // 1. Tìm Order đang PENDING
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId));

        if (!order.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("Order is not pending");
        }

        // 2. Tìm OrderDetail cần xóa trong Set
        OrderDetail detailRemove = order.getOrderDetails().stream()
                .filter(detail -> detail.getId().equals(orderDetailId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("OrderDetail not found with id: " + orderDetailId));

        // 3. Xoá OrderDetail khỏi Set
        order.getOrderDetails().remove(detailRemove);

        // 4. Kiểm tra xem Order còn món nào không
        if (order.getOrderDetails().isEmpty()) {

            // Làm sau
            recalculateOrderTotals(order);
        } else {
            recalculateOrderTotals(order);
        }

        // 5. Lưu lại Order
        Order updatedOrder = orderRepository.save(order);

        // 6. Trả về DTO
        Order fetchedOrder = orderRepository.findById(updatedOrder.getId())
                .orElseThrow(() -> new EntityNotFoundException("Failed to fetch order after removing item"));

        return orderMapper.entityToResponse(fetchedOrder);
    }

    /**
     * Cập nhật thông tin một món (OrderDetail) trong Order đang PENDING
     * (Chủ yếu là số lượng và ghi chú)
     */
    @Transactional
    public OrderResponseDTO updateItemInOrder(Long orderId, Long orderDetailId, OrderDetailUpdateRequestDTO updateDTO) {
        // 1. Tìm Order đang PENDING
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not pending");
        }

        // 2. Tìm OrderDetail cần cập nhật trong Set của Order
        OrderDetail detailToUpdate = order.getOrderDetails().stream()
                .filter(detail -> detail.getId().equals(orderDetailId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("OrderDetail not found with id: " + orderDetailId));

        // 3. Cập nhật số lượng và ghi chú
        detailToUpdate.setQuantity(updateDTO.getQuantity());
        detailToUpdate.setNotes(updateDTO.getNotes());

        recalculateOrderTotals(order);

        Order updatedOrder = orderRepository.save(order);

        Order fetchedOrder = orderRepository.findById(updatedOrder.getId())
                .orElseThrow(() -> new EntityNotFoundException("Failed to fetch order after updating item"));

        return orderMapper.entityToResponse(fetchedOrder);
    }


    /**
     * (Thêm sau) Thanh toán Order
     */
    @Transactional
    public OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest) {
        // 1. Tìm Order đang PENDING
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId));

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not pending");
        }

        // 2. Chuẩn hoá và kiểm tra phương thức thanh toán
        String paymentMethod = paymentRequest.getPaymentMethod().toUpperCase();
        if (!paymentMethod.equals("CASH") &&  !paymentMethod.equals("TRANSFER") && !paymentMethod.equals("CARD")) {
            throw new IllegalArgumentException("Payment method not supported");
        }

        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setPaidAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        if (updatedOrder.getCafeTable() != null) {
            CafeTable cafeTable = updatedOrder.getCafeTable();
            if ("SERVING".equals(cafeTable.getStatus())) {
                cafeTable.setStatus("EMPTY");
                cafeTableRepository.save(cafeTable);
            }
        }

        Order fetchedPaidOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Failed to fetch paid order with id: " + orderId));
        // --- (PHẦN NÂNG CAO - SẼ LÀM SAU) ---
        // 6. Trừ kho nguyên vật liệu
        // 7. Cộng điểm khách hàng (nếu có)
        // ------------------------------------

        return orderMapper.entityToResponse(fetchedPaidOrder);
    }

    /**
     * (Thêm sau) Huỷ Order
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        // 1. Tìm Order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with Id: " + orderId));

        // 2. Chỉ cho phép huỷ đơn PENDING
        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalArgumentException("Order is not pending");
        }

        order.setStatus("CANCELLED");

        // 3. Lưu lại Order
        Order cancelledOrder = orderRepository.save(order);

        // 3. Cập nhật trạng thái bàn, trả về EMPTY
        if (cancelledOrder.getCafeTable() != null) {
            CafeTable cafeTable = cancelledOrder.getCafeTable();

            boolean hasOtherPendingOrder = orderRepository.findPendingOrderByTableId(cafeTable.getId())
                    .filter(otherOrder -> !otherOrder.getId().equals(orderId)) // Lọc bỏ chính đơn vừa huỷ
                    .isPresent();

            if (!hasOtherPendingOrder && "SERVING".equals(cafeTable.getStatus())) {
                cafeTable.setStatus("EMPTY");
                cafeTableRepository.save(cafeTable);
            }
        }

        // --- (PHẦN NÂNG CAO - SẼ LÀM SAU) ---
        // 6. Hoàn trả kho (nếu đã trừ khi tạo đơn - thường thì không)
        // 7. Hoàn trả điểm (nếu đã cộng)
        // ------------------------------------

        // 8. Trả về DTO
        return orderMapper.entityToResponse(cancelledOrder);
    }

    /**
     * Lấy đơn hàng đang PENDING của một bàn
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getPendingOrderByTable(Long tableId) {
        Order order = orderRepository.findPendingOrderByTableId(tableId)
                .orElseThrow(() -> new EntityNotFoundException("No pending order found for table id: " + tableId));
        return orderMapper.entityToResponse(order);
    }
}
