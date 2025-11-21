package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderDetailRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.BestSellerDTO;
import com.giapho.coffee_shop_backend.dto.CustomerAnalyticsDTO;
import com.giapho.coffee_shop_backend.dto.StaffPerformanceDTO;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.exception.dashboard.DashboardInvalidDateRangeException;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardAnalyticsServiceImpl implements DashboardAnalyticsService {

    private static final int DEFAULT_TOP_LIMIT = 5;
    private static final OrderStatus STATUS_PAID = OrderStatus.PAID;
    private static final OrderStatus STATUS_CANCELLED = OrderStatus.CANCELLED;

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardMetricsDTO collectMetrics(LocalDate from, LocalDate to,
                                              boolean includeTopProducts,
                                              boolean includeVoucherStats,
                                              boolean includeCustomerStats) {
        validateRange(from, to);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        List<Order> paidOrders = orderRepository.findByStatusAndDateRange(STATUS_PAID, start, end);
        List<Order> cancelledOrders = orderRepository.findByStatusAndDateRange(STATUS_CANCELLED, start, end);

        long paidCount = paidOrders.size();
        long cancelledCount = cancelledOrders.size();
        long totalOrders = paidCount + cancelledCount;

        BigDecimal totalRevenue = sumNullable(paidOrders.stream().map(Order::getTotalAmount).toList());
        BigDecimal totalDiscount = sumNullable(paidOrders.stream().map(Order::getDiscountAmount).toList());
        BigDecimal averageOrderValue = paidCount > 0
                ? totalRevenue.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long voucherUsageCount = includeVoucherStats
                ? paidOrders.stream().filter(order -> order.getVoucherCode() != null && !order.getVoucherCode().isBlank()).count()
                : 0L;

        List<DashboardMetricsDTO.BestSellerMetric> topProducts = includeTopProducts
                ? mapTopProducts(start, end)
                : Collections.emptyList();

        List<DashboardMetricsDTO.CustomerMetric> topCustomers = includeCustomerStats
                ? mapTopCustomers(start, end)
                : Collections.emptyList();

        List<DashboardMetricsDTO.StaffMetric> topStaff = mapTopStaff(start, end);

        return DashboardMetricsDTO.builder()
                .from(from)
                .to(to)
                .totalOrders(totalOrders)
                .paidOrders(paidCount)
                .cancelledOrders(cancelledCount)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .totalDiscount(totalDiscount)
                .voucherUsageCount(voucherUsageCount)
                .topProducts(topProducts)
                .topCustomers(topCustomers)
                .topStaff(topStaff)
                .build();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new DashboardInvalidDateRangeException("Ngày bắt đầu và kết thúc không được để trống");
        }
        if (from.isAfter(to)) {
            throw new DashboardInvalidDateRangeException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
    }

    private List<DashboardMetricsDTO.BestSellerMetric> mapTopProducts(LocalDateTime start, LocalDateTime end) {
        List<BestSellerDTO> bestSellers = orderDetailRepository
                .findBestSellersByQuantityBetweenDates(start, end, PageRequest.of(0, DEFAULT_TOP_LIMIT));
        if (CollectionUtils.isEmpty(bestSellers)) {
            return Collections.emptyList();
        }
        return bestSellers.stream()
                .map(dto -> DashboardMetricsDTO.BestSellerMetric.builder()
                        .productId(dto.getProductId())
                        .productName(dto.getProductName())
                        .totalQuantity(dto.getTotalQuantitySold() == null ? 0 : dto.getTotalQuantitySold())
                        .totalRevenue(dto.getTotalRevenueGenerated() == null ? BigDecimal.ZERO : dto.getTotalRevenueGenerated())
                        .build())
                .toList();
    }

    private List<DashboardMetricsDTO.CustomerMetric> mapTopCustomers(LocalDateTime start, LocalDateTime end) {
        List<CustomerAnalyticsDTO> customers = customerRepository
                .findTopCustomersBetweenDates(start, end, PageRequest.of(0, DEFAULT_TOP_LIMIT));
        if (CollectionUtils.isEmpty(customers)) {
            return Collections.emptyList();
        }
        return customers.stream()
                .map(dto -> DashboardMetricsDTO.CustomerMetric.builder()
                        .customerId(dto.getCustomerId())
                        .customerName(dto.getFullName())
                        .phone(dto.getPhone())
                        .orderCount(dto.getTotalOrders() == null ? 0 : dto.getTotalOrders())
                        .totalSpend(dto.getTotalSpent() == null ? BigDecimal.ZERO : dto.getTotalSpent())
                        .averageSpend(dto.getAverageSpent() == null ? BigDecimal.ZERO : dto.getAverageSpent())
                        .build())
                .toList();
    }

    private List<DashboardMetricsDTO.StaffMetric> mapTopStaff(LocalDateTime start, LocalDateTime end) {
        List<StaffPerformanceDTO> staffPerformance = userRepository
                .findStaffPerformanceBetweenDates(start, end, PageRequest.of(0, DEFAULT_TOP_LIMIT));
        if (CollectionUtils.isEmpty(staffPerformance)) {
            return Collections.emptyList();
        }
        return staffPerformance.stream()
                .map(dto -> DashboardMetricsDTO.StaffMetric.builder()
                        .staffId(dto.getUserId())
                        .staffName(dto.getFullName() != null ? dto.getFullName() : dto.getUsername())
                        .orderCount(dto.getTotalOrders() == null ? 0 : dto.getTotalOrders())
                        .totalRevenue(dto.getTotalRevenue() == null ? BigDecimal.ZERO : dto.getTotalRevenue())
                        .build())
                .toList();
    }

    private BigDecimal sumNullable(List<BigDecimal> values) {
        if (CollectionUtils.isEmpty(values)) {
            return BigDecimal.ZERO;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
