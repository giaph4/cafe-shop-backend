package com.giapho.coffee_shop_backend.service.shift.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.ShiftReport;
import com.giapho.coffee_shop_backend.domain.entity.ShiftSession;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftReportRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftSessionRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportPaymentBreakdownDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportProductDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftReportResponseDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftReportNotFoundException;
import com.giapho.coffee_shop_backend.service.shift.ShiftReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ShiftReportServiceImpl implements ShiftReportService {

    private final ShiftReportRepository shiftReportRepository;
    private final ShiftSessionRepository shiftSessionRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    private static final int TOP_PRODUCTS_LIMIT = 5;

    @Override
    public ShiftReportResponseDTO generateReport(ShiftSession session, List<Order> orders) {
        Objects.requireNonNull(session, "session must not be null");
        List<Order> orderList = orders == null ? List.of() : orders;

        int totalOrders = orderList.size();
        BigDecimal totalPaidAmount = sumPaidAmount(orderList);
        BigDecimal totalUnpaidAmount = sumUnpaidAmount(orderList);
        int transferredOrders = (int) orderList.stream().filter(Order::isTransferred).count();

        List<ShiftReportPaymentBreakdownDTO> paymentBreakdown = buildPaymentBreakdown(orderList);
        List<ShiftReportProductDTO> topProducts = buildTopProducts(orderList);

        String reportJson = serializeReportJson(paymentBreakdown, topProducts, transferredOrders);

        ShiftReport persisted = shiftReportRepository.findBySession_Id(session.getId())
                .map(existing -> updateExistingReport(existing, totalOrders, totalPaidAmount, totalUnpaidAmount, reportJson))
                .orElseGet(() -> createNewReport(session, totalOrders, totalPaidAmount, totalUnpaidAmount, reportJson));

        ShiftReport saved = shiftReportRepository.save(persisted);
        ShiftReportPayload payload = new ShiftReportPayload(paymentBreakdown, topProducts, transferredOrders);
        return mapToDto(saved, session, payload);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftReportResponseDTO getReport(Long sessionId) {
        ShiftReport report = shiftReportRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new ShiftReportNotFoundException(sessionId));
        ShiftSession session = ensureSessionLoaded(report.getSession(), sessionId);
        ShiftReportPayload payload = deserializePayload(report.getReportJson());
        return mapToDto(report, session, payload);
    }

    @Override
    public ShiftReportResponseDTO getOrGenerate(Long sessionId) {
        return shiftReportRepository.findBySession_Id(sessionId)
                .map(report -> {
                    ShiftSession session = ensureSessionLoaded(report.getSession(), sessionId);
                    ShiftReportPayload payload = deserializePayload(report.getReportJson());
                    return mapToDto(report, session, payload);
                })
                .orElseGet(() -> {
                    ShiftSession session = loadSession(sessionId);
                    List<Order> orders = orderRepository.findByShiftSessionIdWithDetails(sessionId);
                    return generateReport(session, orders);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftReportResponseDTO> listReportsByWorkShift(Long workShiftId) {
        return shiftReportRepository.findBySession_WorkShift_Id(workShiftId).stream()
                .map(report -> {
                    ShiftSession session = ensureSessionLoaded(report.getSession(), report.getSession().getId());
                    ShiftReportPayload payload = deserializePayload(report.getReportJson());
                    return mapToDto(report, session, payload);
                })
                .toList();
    }

    @Override
    public ShiftReportResponseDTO regenerateReport(Long sessionId) {
        ShiftSession session = loadSession(sessionId);
        List<Order> orders = orderRepository.findByShiftSessionIdWithDetails(sessionId);
        return generateReport(session, orders);
    }

    private ShiftReport createNewReport(ShiftSession session,
                                        int totalOrders,
                                        BigDecimal totalPaidAmount,
                                        BigDecimal totalUnpaidAmount,
                                        String reportJson) {
        return ShiftReport.builder()
                .session(session)
                .totalOrders(totalOrders)
                .totalPaidAmount(totalPaidAmount)
                .totalUnpaidAmount(totalUnpaidAmount)
                .reportJson(reportJson)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private ShiftReport updateExistingReport(ShiftReport existing,
                                             int totalOrders,
                                             BigDecimal totalPaidAmount,
                                             BigDecimal totalUnpaidAmount,
                                             String reportJson) {
        existing.setTotalOrders(totalOrders);
        existing.setTotalPaidAmount(totalPaidAmount);
        existing.setTotalUnpaidAmount(totalUnpaidAmount);
        existing.setReportJson(reportJson);
        existing.setGeneratedAt(LocalDateTime.now());
        return existing;
    }

    private ShiftReportResponseDTO mapToDto(ShiftReport report,
                                            ShiftSession session,
                                            ShiftReportPayload payload) {
        return new ShiftReportResponseDTO(
                report.getId(),
                session.getId(),
                session.getWorkShift() != null ? session.getWorkShift().getId() : null,
                session.getUser() != null ? session.getUser().getId() : null,
                session.getUser() != null ? session.getUser().getUsername() : null,
                session.getStatus(),
                session.getStartAt(),
                session.getEndAt(),
                report.getTotalOrders(),
                report.getTotalPaidAmount(),
                report.getTotalUnpaidAmount(),
                payload.transferredOrders(),
                payload.paymentBreakdown(),
                payload.products(),
                report.getGeneratedAt()
        );
    }

    private BigDecimal sumPaidAmount(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumUnpaidAmount(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getStatus() != OrderStatus.PAID)
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ShiftReportPaymentBreakdownDTO> buildPaymentBreakdown(List<Order> orders) {
        Map<String, List<Order>> grouped = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .collect(Collectors.groupingBy(order -> order.getPaymentMethod() == null ? "UNKNOWN" : order.getPaymentMethod()));

        return grouped.entrySet().stream()
                .map(entry -> new ShiftReportPaymentBreakdownDTO(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(Order::getTotalAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted(Comparator.comparing(ShiftReportPaymentBreakdownDTO::paymentMethod))
                .toList();
    }

    private List<ShiftReportProductDTO> buildTopProducts(List<Order> orders) {
        Map<Long, ProductAggregate> productMap = orders.stream()
                .flatMap(order -> {
                    if (order.getOrderDetails() == null) {
                        return java.util.stream.Stream.<OrderDetail>empty();
                    }
                    return order.getOrderDetails().stream();
                })
                .collect(Collectors.toMap(
                        detail -> detail.getProduct().getId(),
                        detail -> new ProductAggregate(
                                detail.getProduct().getId(),
                                detail.getProduct().getName(),
                                detail.getQuantity(),
                                calculateDetailAmount(detail)
                        ),
                        ProductAggregate::merge
                ));

        return productMap.values().stream()
                .sorted(Comparator.comparing(ProductAggregate::quantity).reversed())
                .limit(TOP_PRODUCTS_LIMIT)
                .map(aggregate -> new ShiftReportProductDTO(
                        aggregate.productId(),
                        aggregate.productName(),
                        aggregate.quantity(),
                        aggregate.totalAmount()
                ))
                .toList();
    }

    private BigDecimal calculateDetailAmount(OrderDetail detail) {
        if (detail.getPriceAtOrder() == null) {
            return BigDecimal.ZERO;
        }
        return detail.getPriceAtOrder()
                .multiply(BigDecimal.valueOf(detail.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String serializeReportJson(List<ShiftReportPaymentBreakdownDTO> breakdown,
                                      List<ShiftReportProductDTO> products,
                                      int transferredOrders) {
        try {
            return objectMapper.writeValueAsString(new ShiftReportPayload(breakdown, products, transferredOrders));
        } catch (JsonProcessingException e) {
            log.warn("Không thể serialize báo cáo ca làm việc", e);
            return "{}";
        }
    }

    private ShiftReportPayload deserializePayload(String json) {
        try {
            return objectMapper.readValue(json, ShiftReportPayload.class);
        } catch (Exception ex) {
            log.warn("Không thể deserialize payload báo cáo", ex);
            return new ShiftReportPayload(List.of(), List.of(), 0);
        }
    }

    private ShiftSession loadSession(Long sessionId) {
        return shiftSessionRepository.findWithUserAndWorkShiftById(sessionId)
                .orElseThrow(() -> new ShiftReportNotFoundException(sessionId));
    }

    private ShiftSession ensureSessionLoaded(ShiftSession session, Long sessionId) {
        if (session == null
                || session.getUser() == null
                || session.getWorkShift() == null) {
            return loadSession(sessionId);
        }
        return session;
    }

    private record ShiftReportPayload(List<ShiftReportPaymentBreakdownDTO> paymentBreakdown,
                                      List<ShiftReportProductDTO> products,
                                      int transferredOrders) {
    }

    private record ProductAggregate(Long productId, String productName, int quantity, BigDecimal totalAmount) {

        ProductAggregate merge(ProductAggregate other) {
            return new ProductAggregate(
                    productId,
                    productName,
                    quantity + other.quantity,
                    totalAmount.add(other.totalAmount)
            );
        }
    }
}
