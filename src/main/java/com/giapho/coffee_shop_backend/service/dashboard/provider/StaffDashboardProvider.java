package com.giapho.coffee_shop_backend.service.dashboard.provider;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.PayrollSummaryRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StaffDashboardProvider {

    private static final int UPCOMING_SHIFT_LIMIT = 5;
    private static final long UPCOMING_DAYS = 7L;
    private static final int ATTENDANCE_WINDOW_DAYS = 7;
    private static final int ON_TIME_STREAK_DAYS = 7;

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final OrderRepository orderRepository;
    private final PayrollSummaryRepository payrollSummaryRepository;

    public StaffDashboardDTO build(Long userId, DateRange range) {
        LocalDate today = LocalDate.now();

        StaffDashboardDTO.PersonalShiftSummary shiftSummary = buildPersonalShiftSummary(userId, today);
        List<StaffDashboardDTO.UpcomingShift> upcomingShifts = buildUpcomingShifts(userId, today);
        StaffDashboardDTO.PerformanceSnapshot performance = buildPerformanceSnapshot(userId, range);
        StaffDashboardDTO.AttendanceStatus attendance = buildAttendanceStatus(userId, today);
        StaffDashboardDTO.PayrollSnapshot payroll = buildPayrollSnapshot(userId);

        return StaffDashboardDTO.builder()
                .shiftSummary(shiftSummary)
                .upcomingShifts(upcomingShifts)
                .performance(performance)
                .attendance(attendance)
                .payroll(payroll)
                .taskReminders(List.of())
                .announcements(List.of())
                .build();
    }

    private StaffDashboardDTO.PersonalShiftSummary buildPersonalShiftSummary(Long userId, LocalDate today) {
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<ShiftAssignment> assignments = shiftAssignmentRepository
                .findByUserIdAndShift_ShiftDateBetween(userId, startOfWeek, endOfWeek);
        Map<Long, List<AttendanceRecord>> attendanceByAssignment = loadAttendanceRecords(assignments);

        int completed = (int) assignments.stream()
                .filter(a -> a.getStatus() == ShiftAssignmentStatus.COMPLETED)
                .count();
        int pending = (int) assignments.stream()
                .filter(a -> a.getStatus() == ShiftAssignmentStatus.SCHEDULED
                        || a.getStatus() == ShiftAssignmentStatus.IN_PROGRESS)
                .count();

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

    private List<StaffDashboardDTO.UpcomingShift> buildUpcomingShifts(Long userId, LocalDate today) {
        return shiftAssignmentRepository
                .findByUserIdAndShift_ShiftDateBetween(userId, today, today.plusDays(UPCOMING_DAYS))
                .stream()
                .sorted(Comparator.comparing(assignment -> assignment.getShift().getShiftDate()))
                .limit(UPCOMING_SHIFT_LIMIT)
                .map(this::toUpcomingShift)
                .toList();
    }

    private StaffDashboardDTO.PerformanceSnapshot buildPerformanceSnapshot(Long userId, DateRange range) {
        LocalDateTime start = range.getStart().atStartOfDay();
        LocalDateTime end = range.getEnd().plusDays(1).atStartOfDay();

        List<Order> orders = orderRepository
                .findOrdersForStaffBetween(userId, OrderStatus.PAID, start, end);

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
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

    private StaffDashboardDTO.AttendanceStatus buildAttendanceStatus(Long userId, LocalDate today) {
        List<ShiftAssignment> recentAssignments = shiftAssignmentRepository
                .findByUserIdAndShift_ShiftDateBetween(userId, today.minusDays(ATTENDANCE_WINDOW_DAYS), today);
        Map<Long, List<AttendanceRecord>> attendanceByAssignment = loadAttendanceRecords(recentAssignments);

        List<AttendanceRecord> records = attendanceByAssignment.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(AttendanceRecord::getCheckInAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();

        boolean currentlyCheckedIn = records.stream().anyMatch(record -> record.getCheckOutAt() == null);
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

        int consecutiveOnTimeDays = calculateConsecutiveOnTimeDays(records, today);

        return StaffDashboardDTO.AttendanceStatus.builder()
                .currentlyCheckedIn(currentlyCheckedIn)
                .lastCheckIn(lastCheckIn)
                .lastCheckOut(lastCheckOut)
                .consecutiveOnTimeDays(consecutiveOnTimeDays)
                .build();
    }

    private StaffDashboardDTO.PayrollSnapshot buildPayrollSnapshot(Long userId) {
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
                .max(Comparator.comparing(summary -> summary.getCycle() != null
                        ? summary.getCycle().getStartDate()
                        : LocalDate.MIN))
                .orElse(summaries.get(0));

        return StaffDashboardDTO.PayrollSnapshot.builder()
                .estimatedCurrentCycle(defaultZero(latest.getTotalNetPayroll()))
                .bonusTotal(defaultZero(latest.getTotalBonus()))
                .penaltyTotal(defaultZero(latest.getTotalPenalty()))
                .adjustmentNet(defaultZero(latest.getTotalAdjustment()))
                .lastCyclePaid(defaultZero(latest.getTotalNetPayroll()))
                .build();
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

    private StaffDashboardDTO.UpcomingShift toUpcomingShift(ShiftAssignment assignment) {
        return StaffDashboardDTO.UpcomingShift.builder()
                .assignmentId(assignment.getId())
                .shiftDate(assignment.getShift().getShiftDate())
                .timeRange(formatTimeRange(assignment.getPlannedStart(), assignment.getPlannedEnd()))
                .role(assignment.getRoleName())
                .status(assignment.getStatus().name())
                .managerNote(assignment.getNotes())
                .build();
    }

    private int calculateConsecutiveOnTimeDays(List<AttendanceRecord> records, LocalDate today) {
        int streak = 0;
        for (int i = 0; i < ON_TIME_STREAK_DAYS; i++) {
            LocalDate targetDate = today.minusDays(i);
            boolean onTime = records.stream()
                    .filter(record -> record.getCheckInAt() != null
                            && record.getCheckInAt().toLocalDate().equals(targetDate))
                    .allMatch(record -> record.getLateMinutes() == null || record.getLateMinutes() == 0);
            if (onTime) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        return (start != null ? start.toString() : "?") + " - " + (end != null ? end.toString() : "?");
    }
}
