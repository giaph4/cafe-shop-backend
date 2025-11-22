package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.TableStatus;
import com.giapho.coffee_shop_backend.domain.repository.CafeTableRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.dto.OrderCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.PaymentRequestDTO;
import com.giapho.coffee_shop_backend.mapper.OrderDetailMapper;
import com.giapho.coffee_shop_backend.mapper.OrderMapper;
import com.giapho.coffee_shop_backend.service.OrderService;
import com.giapho.coffee_shop_backend.service.PaymentService;
import com.giapho.coffee_shop_backend.service.order.OrderPricingService;
import com.giapho.coffee_shop_backend.service.order.OrderQueryService;
import com.giapho.coffee_shop_backend.service.order.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CafeTableRepository cafeTableRepository;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderValidator orderValidator;
    private final OrderPricingService orderPricingService;
    private final PaymentService paymentService;
    private final OrderQueryService orderQueryService;

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id) {
        return orderQueryService.getOrderDetail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getPendingOrderByTable(Long tableId) {
        return orderQueryService.getPendingOrderByTable(tableId);
    }

    @Override
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO request) {
        User currentUser = orderValidator.requireCurrentUser();
        Order order = Order.builder()
                .user(currentUser)
                .customer(orderValidator.resolveCustomer(request.getCustomerId()))
                .type(orderValidator.parseOrderType(request.getType()))
                .status(OrderStatus.PENDING)
                .voucherCode(StringUtils.hasText(request.getVoucherCode()) ? request.getVoucherCode().trim().toUpperCase() : null)
                .discountAmount(BigDecimal.ZERO)
                .subTotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        CafeTable table = orderValidator.validateTableForNewOrder(request.getTableId());
        order.setCafeTable(table);

        Map<Long, OrderDetail> mergedDetails = new HashMap<>();
        request.getItems().forEach(item -> mergeOrderDetail(order, mergedDetails, item));
        order.setOrderDetails(new HashSet<>(mergedDetails.values()));

        orderPricingService.recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);
        updateTableStatusOnOrderCreate(table);

        Order reloaded = orderValidator.requireOrderWithDetails(savedOrder.getId());
        return orderMapper.entityToResponse(reloaded);
    }

    @Override
    @Transactional
    public OrderResponseDTO addItemToOrder(Long orderId, OrderDetailRequestDTO itemDTO) {
        Order order = orderValidator.requirePendingOrder(orderId);
        Map<Long, OrderDetail> detailMap = order.getOrderDetails().stream()
                .collect(Collectors.toMap(detail -> detail.getProduct().getId(), detail -> detail));

        mergeOrderDetail(order, detailMap, itemDTO);
        order.setOrderDetails(new HashSet<>(detailMap.values()));

        orderPricingService.recalculateTotals(order);
        orderRepository.save(order);

        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    @Override
    @Transactional
    public OrderResponseDTO updateItemInOrder(Long orderId, Long orderDetailId, OrderDetailUpdateRequestDTO updateDTO) {
        Order order = orderValidator.requirePendingOrder(orderId);
        OrderDetail detail = orderValidator.requireOrderDetail(order, orderDetailId);

        detail.setQuantity(updateDTO.getQuantity());
        detail.setNotes(updateDTO.getNotes());

        orderPricingService.recalculateTotals(order);
        orderRepository.save(order);

        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    @Override
    @Transactional
    public OrderResponseDTO removeItemFromOrder(Long orderId, Long orderDetailId) {
        Order order = orderValidator.requirePendingOrder(orderId);
        OrderDetail detail = orderValidator.requireOrderDetail(order, orderDetailId);

        order.getOrderDetails().remove(detail);
        if (order.getOrderDetails().isEmpty()) {
            orderPricingService.removeVoucher(order);
        }
        orderPricingService.recalculateTotals(order);
        orderRepository.save(order);

        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    @Override
    @Transactional
    public OrderResponseDTO payOrder(Long orderId, PaymentRequestDTO paymentRequest) {
        Order paidOrder = paymentService.processPayment(orderId, paymentRequest);
        updateTableStatusOnOrderCompletion(paidOrder.getCafeTable());
        Order reloaded = orderValidator.requireOrderWithDetails(paidOrder.getId());
        return orderMapper.entityToResponse(reloaded);
    }

    @Override
    @Transactional
    public OrderResponseDTO applyVoucher(Long orderId, String voucherCode) {
        Order order = orderValidator.requirePendingOrder(orderId);
        orderPricingService.applyVoucher(order, voucherCode);
        orderRepository.save(order);
        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    @Override
    @Transactional
    public OrderResponseDTO removeVoucher(Long orderId) {
        Order order = orderValidator.requirePendingOrder(orderId);
        orderPricingService.removeVoucher(order);
        orderRepository.save(order);
        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    @Override
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = orderValidator.requirePendingOrder(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderPricingService.removeVoucher(order);
        orderRepository.save(order);
        updateTableStatusOnOrderCompletion(order.getCafeTable());
        return orderMapper.entityToResponse(orderValidator.requireOrderWithDetails(orderId));
    }

    private void mergeOrderDetail(Order order, Map<Long, OrderDetail> detailMap, OrderDetailRequestDTO itemDTO) {
        Product product = orderValidator.requireAvailableProduct(itemDTO.getProductId());
        OrderDetail existing = detailMap.get(product.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + itemDTO.getQuantity());
            if (StringUtils.hasText(itemDTO.getNotes())) {
                existing.setNotes(itemDTO.getNotes());
            }
            return;
        }

        OrderDetail newDetail = orderDetailMapper.requestToEntity(itemDTO);
        newDetail.setOrder(order);
        newDetail.setProduct(product);
        newDetail.setPriceAtOrder(product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO);
        detailMap.put(product.getId(), newDetail);
    }

    private void updateTableStatusOnOrderCreate(CafeTable table) {
        if (table != null && table.getStatus() == TableStatus.EMPTY) {
            table.setStatus(TableStatus.SERVING);
            cafeTableRepository.save(table);
        }
    }

    private void updateTableStatusOnOrderCompletion(CafeTable table) {
        if (table == null) {
            return;
        }
        boolean hasOtherPending = orderRepository.findByTableIdAndStatus(table.getId(), OrderStatus.PENDING).isPresent();
        if (!hasOtherPending && table.getStatus() == TableStatus.SERVING) {
            table.setStatus(TableStatus.EMPTY);
            cafeTableRepository.save(table);
        }
    }
}
