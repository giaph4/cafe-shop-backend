package com.giapho.coffee_shop_backend.service.dashboard;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.PayrollCycle;
import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.PayrollSummaryRepository;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftInstanceRepository;
import com.giapho.coffee_shop_backend.domain.repository.SupplierRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import com.giapho.coffee_shop_backend.service.ReportService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = {
        "adminDashboard",
        "managerDashboard",
        "staffDashboard"
})
public class RoleDashboardService {

    private static final String STATUS_PENDING = "PENDING";
    private static final long MANAGER_SHIFT_LOOKAHEAD_DAYS = 3L;
    private static final long STAFF_UPCOMING_DAYS = 7L;
    private static final long STAFF_PERFORMANCE_WINDOW_DAYS = 30L;

    private final ReportService reportService;
    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final OrderRepository orderRepository;
    private final IngredientRepository ingredientRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftInstanceRepository shiftInstanceRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final PayrollSummaryRepository payrollSummaryRepository;
    private final UserRepository userRepository;

    public AdminDashboardDTO buildAdminDashboard() {
        return buildAdminDashboard(null, null, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "adminDashboard", key = "#range + '-' + #from + '-' + #to")
    public AdminDashboardDTO buildAdminDashboard(@Nullable DashboardRange range, @Nullable LocalDate from, @Nullable LocalDate to) {
        DateRange effectiveRange = DateRange.from(range, from, to);

        AdminDashboardDTO.RevenueSnapshot revenue = buildAdminRevenueSnapshot(effectiveRange);
        AdminDashboardDTO.OrderSnapshot orders = buildAdminOrderSnapshot(effectiveRange);
        AdminDashboardDTO.InventorySnapshot inventory = buildAdminInventorySnapshot();

        DashboardMetricsDTO metrics = dashboardAnalyticsService.collectMetrics(
                effectiveRange.getStart(),
                effectiveRange.getEnd(),
                true,
                true,
                true
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

        List<AdminDashboardDTO.SystemAlert> alerts = buildAdminAlerts(inventory.lowStockItems(), orders.cancelledToday());

        return AdminDashboardDTO.builder()
                .revenue(revenue)
                .orders(orders)
                .inventory(inventory)
                .topStaff(topStaff)
                .topProducts(topProducts)
                .topCustomers(topCustomers)
                .alerts(alerts)
                .build();
    }

    public ManagerDashboardDTO buildManagerDashboard() {
        return buildManagerDashboard(null, null, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "managerDashboard", key = "#range + '-' + #from + '-' + #to")
    public ManagerDashboardDTO buildManagerDashboard(@Nullable DashboardRange range, @Nullable LocalDate from, @Nullable LocalDate to) {
        DateRange effectiveRange = DateRange.from(range, from, to);
        LocalDate today = effectiveRange.getEnd();

        ManagerDashboardDTO.ShiftOverview shiftOverview = buildManagerShiftOverview(today);
        ManagerDashboardDTO.TeamPerformance teamPerformance = buildManagerTeamPerformance(effectiveRange);
        ManagerDashboardDTO.InventoryFocus inventoryFocus = buildManagerInventoryFocus();
        ManagerDashboardDTO.PayrollOverview payrollOverview = buildManagerPayrollOverview();
        List<ManagerDashboardDTO.PendingApproval> pendingApprovals = buildManagerPendingApprovals();
        List<ManagerDashboardDTO.AttendanceAlert> attendanceAlerts = buildManagerAttendanceAlerts(today);
        List<ManagerDashboardDTO.ServiceIssue> serviceIssues = buildManagerServiceIssues(today);

        return ManagerDashboardDTO.builder()
                .shiftOverview(shiftOverview)
                .teamPerformance(teamPerformance)
                .inventory(inventoryFocus)
                .payroll(payrollOverview)
                .pendingApprovals(pendingApprovals)
                .attendanceAlerts(attendanceAlerts)
                .serviceIssues(serviceIssues)
                .build();
    }

    public StaffDashboardDTO buildStaffDashboard(Long userId) {
        return buildStaffDashboard(userId, null, null, null);
    }

    public StaffDashboardDTO buildStaffDashboard() {
        return buildStaffDashboard(null, null, null, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "staffDashboard", key = "#userId != null ? #userId + '-' + #range + '-' + #from + '-' + #to : 'current-' + #range + '-' + #from + '-' + #to")
    public StaffDashboardDTO buildStaffDashboard(@Nullable Long userId, @Nullable DashboardRange range, @Nullable LocalDate from, @Nullable LocalDate to) {
        DateRange effectiveRange = DateRange.from(range, from, to);
        Long effectiveUserId = userId != null ? userId : resolveCurrentUserId();

        StaffDashboardDTO.PersonalShiftSummary shiftSummary = buildStaffPersonalShiftSummary(effectiveUserId);
        List<StaffDashboardDTO.UpcomingShift> upcomingShifts = buildStaffUpcomingShifts(effectiveUserId);
        StaffDashboardDTO.PerformanceSnapshot performance = buildStaffPerformanceSnapshot(effectiveUserId, effectiveRange);
        StaffDashboardDTO.AttendanceStatus attendanceStatus = buildStaffAttendanceStatus(effectiveUserId);
        StaffDashboardDTO.PayrollSnapshot payrollSnapshot = buildStaffPayrollSnapshot(effectiveUserId);

        return StaffDashboardDTO.builder()
                .shiftSummary(shiftSummary)
                .upcomingShifts(upcomingShifts)
                .performance(performance)
                .attendance(attendanceStatus)
                .payroll(payrollSnapshot)
                .taskReminders(List.of())
                .announcements(List.of())
                .build();
    }

    private AdminDashboardDTO.RevenueSnapshot buildAdminRevenueSnapshot(DateRange range) {
        LocalDate today = range.getEnd();
        BigDecimal todayRevenue = defaultZero(reportService.getDailyRevenue(today));

        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfNextMonth = firstDayOfMonth.plusMonths(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate firstDayOfNextYear = firstDayOfYear.plusYears(1);

        BigDecimal monthRevenue = defaultZero(orderRepository.sumPaidRevenueBetween(
                firstDayOfMonth.atStartOfDay(),
                firstDayOfNextMonth.atStartOfDay()));
        BigDecimal yearRevenue = defaultZero(orderRepository.sumPaidRevenueBetween(
                firstDayOfYear.atStartOfDay(),
                firstDayOfNextYear.atStartOfDay()));

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        Long todayOrdersValue = orderRepository.countPaidOrdersBetween(startOfToday, startOfTomorrow);
        long todayOrders = todayOrdersValue != null ? todayOrdersValue : 0L;
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

    private AdminDashboardDTO.OrderSnapshot buildAdminOrderSnapshot(DateRange range) {
        LocalDate today = range.getEnd();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate firstDayOfNextMonth = firstDayOfMonth.plusMonths(1);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate firstDayOfNextYear = firstDayOfYear.plusYears(1);

        long todayOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()));
        long monthOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                firstDayOfMonth.atStartOfDay(),
                firstDayOfNextMonth.atStartOfDay()));
        long yearOrders = defaultZero(orderRepository.countPaidOrdersBetween(
                firstDayOfYear.atStartOfDay(),
                firstDayOfNextYear.atStartOfDay()));

        long cancelledToday = orderRepository.findByStatusAndDateRange(
                        "CANCELLED",
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay())
                .size();

        long cancelledMonth = orderRepository.findByStatusAndDateRange(
                        "CANCELLED",
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

    private AdminDashboardDTO.InventorySnapshot buildAdminInventorySnapshot() {
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

    private List<AdminDashboardDTO.SystemAlert> buildAdminAlerts(int lowStockItems, long cancelledToday) {
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

    private ManagerDashboardDTO.ShiftOverview buildManagerShiftOverview(LocalDate today) {
        List<ShiftInstance> todayInstances = deduplicateShiftInstances(
                shiftInstanceRepository.findWithTemplateAndAssignmentsBetween(today, today)
        );
        int scheduledToday = todayInstances.size();
        int locked = (int) todayInstances.stream().filter(instance -> instance.getStatus() == ShiftStatus.LOCKED).count();
        int completed = shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.COMPLETED).size();
        int inProgress = shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.IN_PROGRESS).size();
        int cancelled = shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.CANCELLED).size();

        List<ShiftInstance> upcomingInstances = deduplicateShiftInstances(
                shiftInstanceRepository.findWithTemplateAndAssignmentsBetween(today, today.plusDays(MANAGER_SHIFT_LOOKAHEAD_DAYS))
        );

        List<ManagerDashboardDTO.ShiftCard> upcomingShifts = upcomingInstances.stream()
                .sorted(Comparator.comparing(ShiftInstance::getShiftDate).thenComparing(ShiftInstance::getStartTime))
                .limit(6)
                .map(instance -> ManagerDashboardDTO.ShiftCard.builder()
                        .shiftId(instance.getId())
                        .shiftDate(instance.getShiftDate())
                        .timeRange(formatTimeRange(instance.getStartTime(), instance.getEndTime()))
                        .status(instance.getStatus().name())
                        .assignedStaff(instance.getAssignments() == null ? 0 : instance.getAssignments().size())
                        .capacity(instance.getTemplate() != null && instance.getTemplate().getRequiredRoles() != null
                                ? instance.getTemplate().getRequiredRoles().size() : 0)
                        .build())
                .toList();

        return ManagerDashboardDTO.ShiftOverview.builder()
                .scheduledToday(scheduledToday)
                .inProgress(inProgress + locked)
                .completed(completed)
                .cancelled(cancelled)
                .upcomingShifts(upcomingShifts)
                .build();
    }

    private List<ShiftInstance> deduplicateShiftInstances(List<ShiftInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(instances.stream()
                .collect(Collectors.toMap(ShiftInstance::getId, Function.identity(), (existing, replacement) -> existing, LinkedHashMap::new))
                .values());
    }

    private ManagerDashboardDTO.TeamPerformance buildManagerTeamPerformance(DateRange range) {
        DashboardMetricsDTO metrics = dashboardAnalyticsService.collectMetrics(
                range.getStart(),
                range.getEnd(),
                false,
                false,
                false
        );

        List<ManagerDashboardDTO.StaffLeaderboardItem> topStaff = metrics.topStaff().stream()
                .map(item -> ManagerDashboardDTO.StaffLeaderboardItem.builder()
                        .staffId(item.staffId())
                        .staffName(item.staffName())
                        .orders(item.orderCount())
                        .revenue(item.totalRevenue())
                        .averageOrderValue(metrics.averageOrderValue())
                        .build())
                .toList();

        return ManagerDashboardDTO.TeamPerformance.builder()
                .totalRevenue(metrics.totalRevenue())
                .totalOrders(Math.toIntExact(metrics.paidOrders()))
                .averageOrderValue(metrics.averageOrderValue())
                .topStaff(topStaff)
                .build();
    }

    private ManagerDashboardDTO.InventoryFocus buildManagerInventoryFocus() {
        var lowStock = ingredientRepository.findIngredientsBelowReorderLevel();
        int critical = (int) lowStock.stream()
                .filter(ingredient -> ingredient.getQuantityOnHand() != null
                        && ingredient.getReorderLevel() != null
                        && ingredient.getReorderLevel().compareTo(BigDecimal.ZERO) > 0
                        && ingredient.getQuantityOnHand()
                                .compareTo(ingredient.getReorderLevel().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) <= 0)
                .count();

        List<ManagerDashboardDTO.InventoryAlert> alerts = lowStock.stream()
                .map(ingredient -> ManagerDashboardDTO.InventoryAlert.builder()
                        .ingredientId(ingredient.getId())
                        .ingredientName(ingredient.getName())
                        .quantityOnHand(ingredient.getQuantityOnHand())
                        .reorderLevel(ingredient.getReorderLevel())
                        .build())
                .toList();

        return ManagerDashboardDTO.InventoryFocus.builder()
                .lowStockItems(lowStock.size())
                .criticalStockItems(critical)
                .alerts(alerts)
                .build();
    }

    private ManagerDashboardDTO.PayrollOverview buildManagerPayrollOverview() {
        List<PayrollSummary> summaries = payrollSummaryRepository.search(null, null);
        if (summaries.isEmpty()) {
            return ManagerDashboardDTO.PayrollOverview.builder()
                    .estimatedPayroll(BigDecimal.ZERO)
                    .bonusTotal(BigDecimal.ZERO)
                    .penaltyTotal(BigDecimal.ZERO)
                    .adjustmentNet(BigDecimal.ZERO)
                    .staffCount(0)
                    .build();
        }

        Optional<PayrollCycle> latestCycleOpt = summaries.stream()
                .map(PayrollSummary::getCycle)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(PayrollCycle::getStartDate));

        List<PayrollSummary> currentSummaries = latestCycleOpt
                .map(cycle -> summaries.stream()
                        .filter(summary -> summary.getCycle() != null
                                && Objects.equals(summary.getCycle().getId(), cycle.getId()))
                        .toList())
                .orElse(summaries);

        BigDecimal estimatedPayroll = currentSummaries.stream()
                .map(PayrollSummary::getTotalNetPayroll)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bonusTotal = currentSummaries.stream()
                .map(PayrollSummary::getTotalBonus)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal penaltyTotal = currentSummaries.stream()
                .map(PayrollSummary::getTotalPenalty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adjustmentNet = currentSummaries.stream()
                .map(PayrollSummary::getTotalAdjustment)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<Long> staffIds = currentSummaries.stream()
                .map(PayrollSummary::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return ManagerDashboardDTO.PayrollOverview.builder()
                .estimatedPayroll(estimatedPayroll)
                .bonusTotal(bonusTotal)
                .penaltyTotal(penaltyTotal)
                .adjustmentNet(adjustmentNet)
                .staffCount(staffIds.size())
                .build();
    }

    private List<ManagerDashboardDTO.PendingApproval> buildManagerPendingApprovals() {
        List<PurchaseOrder> pendingPurchaseOrders = purchaseOrderRepository
                .findByStatus(STATUS_PENDING, PageRequest.of(0, 10))
                .getContent();

        return pendingPurchaseOrders.stream()
                .map(order -> ManagerDashboardDTO.PendingApproval.builder()
                        .module("PURCHASE_ORDER")
                        .description("Phiếu nhập " + order.getId() + " - "
                                + (order.getSupplier() != null ? order.getSupplier().getName() : "Nhà cung cấp không xác định"))
                        .requestedBy(order.getUser() != null ? order.getUser().getUsername() : "SYSTEM")
                        .requestedAt(order.getOrderDate() != null ? order.getOrderDate().toLocalDate() : LocalDate.now())
                        .status(order.getStatus())
                        .build())
                .toList();
    }

    private List<ManagerDashboardDTO.AttendanceAlert> buildManagerAttendanceAlerts(LocalDate today) {
        List<ShiftAssignment> assignments = shiftAssignmentRepository
                .findByShift_ShiftDateBetween(today.minusDays(1), today.plusDays(1));

        Map<Long, List<AttendanceRecord>> attendanceByAssignment = loadAttendanceRecordsForAssignments(assignments);
        List<ManagerDashboardDTO.AttendanceAlert> alerts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (ShiftAssignment assignment : assignments) {
            List<AttendanceRecord> records = attendanceByAssignment.getOrDefault(assignment.getId(), List.of());
            boolean hasOpenCheckIn = records.stream().anyMatch(record -> record.getCheckOutAt() == null);
            boolean hasLate = records.stream().anyMatch(record -> record.getLateMinutes() != null && record.getLateMinutes() > 0);
            boolean hasEarlyLeave = records.stream().anyMatch(record -> record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0);

            if (!hasOpenCheckIn && records.isEmpty() && assignment.getShift().getShiftDate().equals(today)) {
                LocalDateTime plannedStart = LocalDateTime.of(assignment.getShift().getShiftDate(), assignment.getPlannedStart());
                if (now.isAfter(plannedStart.plusMinutes(15))) {
                    alerts.add(ManagerDashboardDTO.AttendanceAlert.builder()
                            .assignmentId(assignment.getId())
                            .staffId(assignment.getUser().getId())
                            .staffName(resolveStaffName(assignment.getUser()))
                            .issueType("NO_CHECK_IN")
                            .note("Chưa check-in sau giờ bắt đầu 15 phút")
                            .build());
                }
            }

            if (hasLate) {
                alerts.add(ManagerDashboardDTO.AttendanceAlert.builder()
                        .assignmentId(assignment.getId())
                        .staffId(assignment.getUser().getId())
                        .staffName(resolveStaffName(assignment.getUser()))
                        .issueType("LATE_CHECK_IN")
                        .note("Có lần check-in trễ trong 24h qua")
                        .build());
            }

            if (hasEarlyLeave) {
                alerts.add(ManagerDashboardDTO.AttendanceAlert.builder()
                        .assignmentId(assignment.getId())
                        .staffId(assignment.getUser().getId())
                        .staffName(resolveStaffName(assignment.getUser()))
                        .issueType("EARLY_CHECK_OUT")
                        .note("Có lần check-out sớm trong 24h qua")
                        .build());
            }
        }

        return alerts.stream()
                .limit(10)
                .toList();
    }

    private List<ManagerDashboardDTO.ServiceIssue> buildManagerServiceIssues(LocalDate today) {
        LocalDateTime start = today.minusDays(3).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        return orderRepository.findByStatusAndDateRange("CANCELLED", start, end).stream()
                .sorted(Comparator.comparing(order -> order.getCreatedAt() == null ? LocalDateTime.MIN : order.getCreatedAt(), Comparator.reverseOrder()))
                .limit(10)
                .map(order -> ManagerDashboardDTO.ServiceIssue.builder()
                        .orderId(order.getId())
                        .tableName(order.getCafeTable() != null ? order.getCafeTable().getName() : "Take Away/Delivery")
                        .issue("Đơn bị hủy")
                        .severity("MEDIUM")
                        .createdDate(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : today)
                        .build())
                .toList();
    }

    private StaffDashboardDTO.PersonalShiftSummary buildStaffPersonalShiftSummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<ShiftAssignment> assignments = shiftAssignmentRepository
                .findByUserIdAndShift_ShiftDateBetween(userId, startOfWeek, endOfWeek);
        Map<Long, List<AttendanceRecord>> attendanceByAssignment = loadAttendanceRecordsForAssignments(assignments);

        int completed = (int) assignments.stream().filter(a -> a.getStatus() == ShiftAssignmentStatus.COMPLETED).count();
        int pending = (int) assignments.stream().filter(a -> a.getStatus() == ShiftAssignmentStatus.SCHEDULED || a.getStatus() == ShiftAssignmentStatus.IN_PROGRESS).count();

        long lateCheckIns = attendanceByAssignment.values().stream()
                .flatMap(List::stream)
                .filter(record -> record.getLateMinutes() != null && record.getLateMinutes() > 0)
                .count();

        long earlyCheckOuts = attendanceByAssignment.values().stream()
                .flatMap(List::stream)
                .filter(record -> record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0)
                .count();

        return StaffDashboardDTO.PersonalShiftSummary.builder()
                .shiftsThisWeek(assignments.size())
                .completedShifts(completed)
                .pendingShifts(pending)
                .lateCheckIns((int) lateCheckIns)
                .earlyCheckOuts((int) earlyCheckOuts)
                .build();
    }

    private List<StaffDashboardDTO.UpcomingShift> buildStaffUpcomingShifts(Long userId) {
        LocalDate today = LocalDate.now();
        List<ShiftAssignment> upcomingAssignments = shiftAssignmentRepository
                .findByUserIdAndShift_ShiftDateBetween(userId, today, today.plusDays(STAFF_UPCOMING_DAYS))
                .stream()
                .sorted(Comparator.comparing(assignment -> assignment.getShift().getShiftDate()))
                .limit(5)
                .toList();

        return upcomingAssignments.stream()
                .map(assignment -> StaffDashboardDTO.UpcomingShift.builder()
                        .assignmentId(assignment.getId())
                        .shiftDate(assignment.getShift().getShiftDate())
                        .timeRange(formatTimeRange(assignment.getPlannedStart(), assignment.getPlannedEnd()))
                        .role(assignment.getRoleName())
                        .status(assignment.getStatus().name())
                        .managerNote(assignment.getNotes())
                        .build())
                .toList();
    }

    private StaffDashboardDTO.PerformanceSnapshot buildStaffPerformanceSnapshot(Long userId, DateRange range) {
        LocalDateTime start = range.getStart().atStartOfDay();
        LocalDateTime end = range.getEnd().plusDays(1).atStartOfDay();

        List<com.giapho.coffee_shop_backend.domain.entity.Order> orders = orderRepository.findPaidOrdersForStaffBetween(userId, start, end);

        BigDecimal totalRevenue = orders.stream()
                .map(com.giapho.coffee_shop_backend.domain.entity.Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = orders.size();
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return StaffDashboardDTO.PerformanceSnapshot.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrderValue(averageOrderValue)
                .positiveFeedbacks(0L)
                .negativeFeedbacks(0L)
                .build();
    }

    private StaffDashboardDTO.AttendanceStatus buildStaffAttendanceStatus(Long userId) {
        LocalDate today = LocalDate.now();
        List<ShiftAssignment> recentAssignments = shiftAssignmentRepository
                .findByUserIdAndShift_ShiftDateBetween(userId, today.minusDays(7), today);
        Map<Long, List<AttendanceRecord>> attendanceByAssignment = loadAttendanceRecordsForAssignments(recentAssignments);

        Comparator<AttendanceRecord> checkInComparator = Comparator.comparing(
                AttendanceRecord::getCheckInAt,
                Comparator.nullsLast(LocalDateTime::compareTo)
        );

        List<AttendanceRecord> records = attendanceByAssignment.values().stream()
                .flatMap(List::stream)
                .sorted(checkInComparator.reversed())
                .toList();

        boolean currentlyCheckedIn = records.stream().anyMatch(att -> att.getCheckOutAt() == null);
        LocalDateTime lastCheckIn = records.stream()
                .map(AttendanceRecord::getCheckInAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime lastCheckOut = records.stream()
                .map(AttendanceRecord::getCheckOutAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        int consecutiveOnTimeDays = calculateConsecutiveOnTimeDays(records);

        return StaffDashboardDTO.AttendanceStatus.builder()
                .currentlyCheckedIn(currentlyCheckedIn)
                .lastCheckIn(lastCheckIn)
                .lastCheckOut(lastCheckOut)
                .consecutiveOnTimeDays(consecutiveOnTimeDays)
                .build();
    }

    private Map<Long, List<AttendanceRecord>> loadAttendanceRecordsForAssignments(List<ShiftAssignment> assignments) {
        if (assignments.isEmpty()) {
            return Map.of();
        }

        List<Long> assignmentIds = assignments.stream()
                .map(ShiftAssignment::getId)
                .toList();

        return attendanceRecordRepository.findByAssignmentIdIn(assignmentIds).stream()
                .collect(Collectors.groupingBy(record -> record.getAssignment().getId(), Collectors.toList()));
    }

    private StaffDashboardDTO.PayrollSnapshot buildStaffPayrollSnapshot(Long userId) {
        List<PayrollSummary> summaries = payrollSummaryRepository.search(null, userId);
        if (summaries.isEmpty()) {
            return StaffDashboardDTO.PayrollSnapshot.builder()
                    .estimatedCurrentCycle(BigDecimal.ZERO)
                    .bonusTotal(BigDecimal.ZERO)
                    .penaltyTotal(BigDecimal.ZERO)
                    .adjustmentNet(BigDecimal.ZERO)
                    .lastCyclePaid(BigDecimal.ZERO)
                    .build();
        }

        PayrollSummary latest = summaries.stream()
                .max(Comparator.comparing(summary -> summary.getCycle() != null ? summary.getCycle().getStartDate() : LocalDate.MIN))
                .orElse(summaries.get(0));

        return StaffDashboardDTO.PayrollSnapshot.builder()
                .estimatedCurrentCycle(defaultZero(latest.getTotalNetPayroll()))
                .bonusTotal(defaultZero(latest.getTotalBonus()))
                .penaltyTotal(defaultZero(latest.getTotalPenalty()))
                .adjustmentNet(defaultZero(latest.getTotalAdjustment()))
                .lastCyclePaid(defaultZero(latest.getTotalNetPayroll()))
                .build();
    }

    @CacheEvict(cacheNames = {
            "adminDashboard",
            "managerDashboard",
            "staffDashboard"
    }, allEntries = true)
    public void evictAllCaches() {
        // invoked on significant data changes (e.g. scheduled cron)
    }

    private Long resolveCurrentUserId() {
        return SecurityUtil.getCurrentUsername()
                .flatMap(username -> userRepository.findByUsername(username).map(User::getId))
                .orElseThrow(() -> new EntityNotFoundException("Không xác định được người dùng hiện tại"));
    }

    private String resolveStaffName(User user) {
        if (user == null) {
            return "UNKNOWN";
        }
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername();
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        return (start != null ? start.toString() : "?") + " - " + (end != null ? end.toString() : "?");
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private long defaultZero(Long value) {
        return value != null ? value : 0L;
    }

    private int calculateConsecutiveOnTimeDays(List<AttendanceRecord> records) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = today.minusDays(i);
            boolean onTime = records.stream()
                    .filter(att -> att.getCheckInAt() != null && att.getCheckInAt().toLocalDate().equals(targetDate))
                    .allMatch(att -> att.getLateMinutes() == null || att.getLateMinutes() == 0);
            if (onTime) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }
}
