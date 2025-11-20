package com.giapho.coffee_shop_backend.service.report.core;

import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.CustomerRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ProductRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.CustomerAnalyticsDTO;
import com.giapho.coffee_shop_backend.dto.DashboardStatsDTO;
import com.giapho.coffee_shop_backend.dto.StaffPerformanceDTO;
import com.giapho.coffee_shop_backend.service.report.helper.ReportCalculationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsReportProvider {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryReportProvider inventoryReportProvider;
    private final RevenueReportProvider revenueReportProvider;

    public List<CustomerAnalyticsDTO> getTopCustomers(LocalDate startDate, LocalDate endDate, int top) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(0, top);
        return customerRepository.findTopCustomersBetweenDates(start, end, pageable);
    }

    public List<StaffPerformanceDTO> getStaffPerformance(LocalDate startDate, LocalDate endDate, int top) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        Pageable pageable = PageRequest.of(0, top);
        return userRepository.findStaffPerformanceBetweenDates(start, end, pageable);
    }

    public DashboardStatsDTO getDashboardStats() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfNextMonth = firstDayOfMonth.plusMonths(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate firstDayOfNextYear = firstDayOfYear.plusYears(1);

        BigDecimal todayRevenue = revenueReportProvider.getDailyRevenue(today);
        BigDecimal monthRevenue = ReportCalculationHelper.defaultZero(orderRepository.sumPaidRevenueBetween(
                OrderStatus.PAID,
                firstDayOfMonth.atStartOfDay(),
                firstDayOfNextMonth.atStartOfDay()));
        BigDecimal yearRevenue = ReportCalculationHelper.defaultZero(orderRepository.sumPaidRevenueBetween(
                OrderStatus.PAID,
                firstDayOfYear.atStartOfDay(),
                firstDayOfNextYear.atStartOfDay()));

        long todayOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                OrderStatus.PAID,
                today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        long monthOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                OrderStatus.PAID,
                firstDayOfMonth.atStartOfDay(), firstDayOfNextMonth.atStartOfDay()));
        long yearOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                OrderStatus.PAID,
                firstDayOfYear.atStartOfDay(), firstDayOfNextYear.atStartOfDay()));

        long totalCustomers = customerRepository.count();
        long totalProducts = productRepository.count();
        int lowStockItems = inventoryReportProvider.getLowStockCount();

        BigDecimal averageOrderValue = todayOrders > 0
                ? ReportCalculationHelper.safeDivide(todayRevenue, BigDecimal.valueOf(todayOrders))
                : BigDecimal.ZERO;

        var todayProfit = revenueReportProvider.getProfitReport(today, today);
        var monthProfit = revenueReportProvider.getProfitReport(firstDayOfMonth, today);

        return DashboardStatsDTO.builder()
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .yearRevenue(yearRevenue)
                .todayOrders(todayOrders)
                .monthOrders(monthOrders)
                .yearOrders(yearOrders)
                .totalCustomers(totalCustomers)
                .totalProducts(totalProducts)
                .lowStockItems(lowStockItems)
                .averageOrderValue(averageOrderValue)
                .todayProfit(todayProfit.getOrDefault("totalProfit", BigDecimal.ZERO))
                .monthProfit(monthProfit.getOrDefault("totalProfit", BigDecimal.ZERO))
                .build();
    }

    private long defaultZero(Long value) {
        return value != null ? value : 0L;
    }
}
