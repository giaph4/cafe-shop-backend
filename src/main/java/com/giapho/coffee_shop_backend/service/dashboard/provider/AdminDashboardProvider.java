package com.giapho.coffee_shop_backend.service.dashboard.provider;

import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import com.giapho.coffee_shop_backend.service.ReportService;
import com.giapho.coffee_shop_backend.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminDashboardProvider {

    private static final String STATUS_PENDING = "PENDING";
    private static final OrderStatus STATUS_PAID = OrderStatus.PAID;
    private static final OrderStatus STATUS_CANCELLED = OrderStatus.CANCELLED;

    private final ReportService reportService;
    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public AdminDashboardDTO build(DateRange range) {
        AdminDashboardDTO.RevenueSnapshot revenue = buildRevenueSnapshot(range);
        AdminDashboardDTO.OrderSnapshot orders = buildOrderSnapshot(range);
        AdminDashboardDTO.InventorySnapshot inventory = buildInventorySnapshot();

        DashboardMetricsDTO metrics = dashboardAnalyticsService.collectMetrics(
                range.getStart(), range.getEnd(), true, true, true
        );

        List<AdminDashboardDTO.TopProductMetric> topProducts = metrics.topProducts().stream()
                .map(item -> AdminDashboardDTO.TopProductMetric.builder()
                        .productId(item.productId())
                        .productName(item.productName())
                        .quantity(item.totalQuantity())
                        .revenue(item.totalRevenue())
                        .build())
                .toList();

        List<AdminDashboardDTO.TopCustomerMetric> topCustomers = metrics.topCustomers().stream()
                .map(item -> AdminDashboardDTO.TopCustomerMetric.builder()
                        .customerId(item.customerId())
                        .customerName(item.customerName())
                        .phone(item.phone())
                        .orders(item.orderCount())
                        .spend(item.totalSpend())
                        .build())
                .toList();

        List<AdminDashboardDTO.TopStaffMetric> topStaff = metrics.topStaff().stream()
                .map(item -> AdminDashboardDTO.TopStaffMetric.builder()
                        .staffId(item.staffId())
                        .staffName(item.staffName())
                        .orders(item.orderCount())
                        .revenue(item.totalRevenue())
                        .build())
                .toList();

        List<AdminDashboardDTO.SystemAlert> alerts = buildAlerts(inventory.lowStockItems(), orders.cancelledToday());

        return AdminDashboardDTO.builder()
                .revenue(revenue)
                .orders(orders)
                .inventory(inventory)
                .topProducts(topProducts)
                .topCustomers(topCustomers)
                .topStaff(topStaff)
                .alerts(alerts)
                .build();
    }

    private AdminDashboardDTO.RevenueSnapshot buildRevenueSnapshot(DateRange range) {
        LocalDate today = range.getEnd();
        BigDecimal todayRevenue = defaultZero(reportService.getDailyRevenue(today));

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfNextMonth = firstDayOfMonth.plusMonths(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate firstDayOfNextYear = firstDayOfYear.plusYears(1);

        BigDecimal monthRevenue = defaultZero(orderRepository.sumPaidRevenueBetween(
                STATUS_PAID,
                firstDayOfMonth.atStartOfDay(),
                firstDayOfNextMonth.atStartOfDay()));
        BigDecimal yearRevenue = defaultZero(orderRepository.sumPaidRevenueBetween(
                STATUS_PAID,
                firstDayOfYear.atStartOfDay(),
                firstDayOfNextYear.atStartOfDay()));

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        long todayOrders = defaultZero(orderRepository.countPaidOrdersBetween(STATUS_PAID, startOfToday, startOfTomorrow));
        BigDecimal averageOrderValue = todayOrders > 0
                ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, BigDecimal> todayProfit = reportService.getProfitReport(today, today);
        Map<String, BigDecimal> periodProfit = reportService.getProfitReport(range.getStart(), range.getEnd());

        return AdminDashboardDTO.RevenueSnapshot.builder()
                .today(todayRevenue)
                .month(monthRevenue)
                .year(yearRevenue)
                .averageOrderValue(averageOrderValue)
                .todayProfit(todayProfit.getOrDefault("totalProfit", BigDecimal.ZERO))
                .monthProfit(periodProfit.getOrDefault("totalProfit", BigDecimal.ZERO))
                .build();
    }

    private AdminDashboardDTO.OrderSnapshot buildOrderSnapshot(DateRange range) {
        LocalDate today = range.getEnd();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfNextMonth = firstDayOfMonth.plusMonths(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate firstDayOfNextYear = firstDayOfYear.plusYears(1);

        long todayOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                STATUS_PAID,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()));
        long monthOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                STATUS_PAID,
                firstDayOfMonth.atStartOfDay(),
                firstDayOfNextMonth.atStartOfDay()));
        long yearOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                STATUS_PAID,
                firstDayOfYear.atStartOfDay(),
                firstDayOfNextYear.atStartOfDay()));

        long cancelledToday = orderRepository.findByStatusAndDateRange(
                        STATUS_CANCELLED,
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay())
                .size();

        long cancelledMonth = orderRepository.findByStatusAndDateRange(
                        STATUS_CANCELLED,
                        firstDayOfMonth.atStartOfDay(),
                        firstDayOfNextMonth.atStartOfDay())
                .size();

        return AdminDashboardDTO.OrderSnapshot.builder()
                .today(todayOrders)
                .month(monthOrders)
                .year(yearOrders)
                .cancelledToday(cancelledToday)
                .cancelledMonth(cancelledMonth)
                .build();
    }

    private AdminDashboardDTO.InventorySnapshot buildInventorySnapshot() {
        int lowStockItems = ingredientRepository.findIngredientsBelowReorderLevel().size();
        int totalSuppliers = (int) supplierRepository.count();
        long pendingPurchaseOrders = purchaseOrderRepository
                .findByStatus(STATUS_PENDING, Pageable.unpaged())
                .getTotalElements();

        return AdminDashboardDTO.InventorySnapshot.builder()
                .lowStockItems(lowStockItems)
                .totalSuppliers(totalSuppliers)
                .pendingPurchaseOrders(Math.toIntExact(pendingPurchaseOrders))
                .build();
    }

    private List<AdminDashboardDTO.SystemAlert> buildAlerts(int lowStockItems, long cancelledToday) {
        List<AdminDashboardDTO.SystemAlert> alerts = new ArrayList<>();

        alerts.add(AdminDashboardDTO.SystemAlert.builder()
                .type("INVENTORY")
                .severity(lowStockItems > 0 ? "HIGH" : "INFO")
                .message(lowStockItems > 0
                        ? "Có " + lowStockItems + " nguyên liệu dưới mức tồn kho an toàn"
                        : "Không có nguyên liệu nào dưới mức tồn kho")
                .build());

        alerts.add(AdminDashboardDTO.SystemAlert.builder()
                .type("ORDER")
                .severity(cancelledToday > 5 ? "MEDIUM" : "INFO")
                .message("Hôm nay có " + cancelledToday + " đơn bị hủy")
                .build());

        return alerts;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long defaultZero(Long value) {
        return value != null ? value : 0L;
    }
}
