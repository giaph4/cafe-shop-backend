package com.giapho.coffee_shop_backend.service.dashboard.provider;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.PayrollCycle;
import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.IngredientRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.PayrollSummaryRepository;
import com.giapho.coffee_shop_backend.domain.repository.PurchaseOrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftInstanceRepository;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import com.giapho.coffee_shop_backend.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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

@Component
@RequiredArgsConstructor
public class ManagerDashboardProvider {

    private static final String STATUS_PENDING = "PENDING";
    private static final long SHIFT_LOOKAHEAD_DAYS = 3L;
    private static final long ATTENDANCE_ALERT_LIMIT = 10L;
    private static final long SERVICE_ISSUE_LOOKBACK_DAYS = 3L;

    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftInstanceRepository shiftInstanceRepository;
    private final IngredientRepository ingredientRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PayrollSummaryRepository payrollSummaryRepository;
    private final OrderRepository orderRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public ManagerDashboardDTO build(DateRange range) {
        LocalDate today = range.getEnd();

        ManagerDashboardDTO.ShiftOverview shiftOverview = buildShiftOverview(today);
        ManagerDashboardDTO.TeamPerformance teamPerformance = buildTeamPerformance(range);
        ManagerDashboardDTO.InventoryFocus inventoryFocus = buildInventoryFocus();
        ManagerDashboardDTO.PayrollOverview payrollOverview = buildPayrollOverview();
        List<ManagerDashboardDTO.PendingApproval> pendingApprovals = buildPendingApprovals();
        List<ManagerDashboardDTO.AttendanceAlert> attendanceAlerts = buildAttendanceAlerts(today);
        List<ManagerDashboardDTO.ServiceIssue> serviceIssues = buildServiceIssues(today);

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

    private ManagerDashboardDTO.ShiftOverview buildShiftOverview(LocalDate today) {
        List<ShiftInstance> todayInstances = deduplicateShiftInstances(
                shiftInstanceRepository.findWithTemplateAndAssignmentsBetween(today, today)
        );
        int scheduledToday = todayInstances.size();
        int locked = (int) todayInstances.stream().filter(instance -> instance.getStatus() == ShiftStatus.LOCKED).count();
        int completed = shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.COMPLETED).size();
        int inProgress = shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.IN_PROGRESS).size();
        int cancelled = shiftAssignmentRepository.findByStatus(ShiftAssignmentStatus.CANCELLED).size();

        List<ShiftInstance> upcomingInstances = deduplicateShiftInstances(
                shiftInstanceRepository.findWithTemplateAndAssignmentsBetween(today, today.plusDays(SHIFT_LOOKAHEAD_DAYS))
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

    private ManagerDashboardDTO.TeamPerformance buildTeamPerformance(DateRange range) {
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

    private ManagerDashboardDTO.InventoryFocus buildInventoryFocus() {
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

    private ManagerDashboardDTO.PayrollOverview buildPayrollOverview() {
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

        BigDecimal estimatedPayroll = sumNullable(currentSummaries, PayrollSummary::getTotalNetPayroll);
        BigDecimal bonusTotal = sumNullable(currentSummaries, PayrollSummary::getTotalBonus);
        BigDecimal penaltyTotal = sumNullable(currentSummaries, PayrollSummary::getTotalPenalty);
        BigDecimal adjustmentNet = sumNullable(currentSummaries, PayrollSummary::getTotalAdjustment);

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

    private List<ManagerDashboardDTO.PendingApproval> buildPendingApprovals() {
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

    private List<ManagerDashboardDTO.AttendanceAlert> buildAttendanceAlerts(LocalDate today) {
        List<ShiftAssignment> assignments = shiftAssignmentRepository
                .findByShift_ShiftDateBetween(today.minusDays(1), today.plusDays(1));

        Map<Long, List<AttendanceRecord>> attendanceByAssignment = loadAttendanceRecords(assignments);
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
                .limit(ATTENDANCE_ALERT_LIMIT)
                .toList();
    }

    private List<ManagerDashboardDTO.ServiceIssue> buildServiceIssues(LocalDate today) {
        LocalDateTime start = today.minusDays(SERVICE_ISSUE_LOOKBACK_DAYS).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        return orderRepository.findByStatusAndDateRange(OrderStatus.CANCELLED, start, end).stream()
                .sorted(Comparator.comparing(
                        (Order order) -> order.getCreatedAt() == null ? LocalDateTime.MIN : order.getCreatedAt(),
                        Comparator.reverseOrder())
                )
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

    private List<AttendanceRecord> flattenAttendance(Map<Long, List<AttendanceRecord>> attendanceByAssignment) {
        return attendanceByAssignment.values().stream().flatMap(List::stream).toList();
    }

    private Map<Long, List<AttendanceRecord>> loadAttendanceRecords(List<ShiftAssignment> assignments) {
        if (assignments.isEmpty()) {
            return Map.of();
        }

        List<Long> assignmentIds = assignments.stream()
                .map(ShiftAssignment::getId)
                .toList();

        return attendanceRecordRepository.findByAssignmentIdIn(assignmentIds).stream()
                .collect(Collectors.groupingBy(record -> record.getAssignment().getId(), Collectors.toList()));
    }

    private List<ShiftInstance> deduplicateShiftInstances(List<ShiftInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(instances.stream()
                .collect(Collectors.toMap(ShiftInstance::getId, Function.identity(), (existing, replacement) -> existing, LinkedHashMap::new))
                .values());
    }

    private BigDecimal sumNullable(List<PayrollSummary> summaries, Function<PayrollSummary, BigDecimal> extractor) {
        return summaries.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
}
