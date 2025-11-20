package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.ShiftPerformanceAdjustment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.AdjustmentType;
import com.giapho.coffee_shop_backend.domain.enums.OrderStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.OrderRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftInstanceRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftPerformanceAdjustmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentUpdateRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentLockedException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentOverlapException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentStateException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftNotFoundException;
import com.giapho.coffee_shop_backend.exception.user.UserNotAuthenticatedException;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import com.giapho.coffee_shop_backend.mapper.ShiftAssignmentMapper;
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftInstanceRepository shiftInstanceRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ShiftPerformanceAdjustmentRepository adjustmentRepository;
    private final ShiftAssignmentMapper shiftAssignmentMapper;

    @Override
    public ShiftAssignmentResponseDTO getAssignment(Long assignmentId) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        return shiftAssignmentMapper.toResponseDTO(assignment);
    }

    @Override
    public List<ShiftAssignmentResponseDTO> getAssignmentsForShift(Long shiftId) {
        return shiftAssignmentRepository.findByShiftId(shiftId).stream()
                .map(shiftAssignmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ShiftAssignmentResponseDTO createAssignment(ShiftAssignmentRequestDTO request) {
        ShiftInstance shift = findShift(request.shiftId());
        ensureShiftIsActive(shift);

        User user = findUser(request.userId());

        validateTimeRange(request.plannedStart(), request.plannedEnd());
        guardOverlap(user.getId(), shift.getShiftDate(), request.plannedStart(), request.plannedEnd(), null);

        ShiftAssignment assignment = shiftAssignmentMapper.toEntity(request);
        assignment.setShift(shift);
        assignment.setUser(user);
        assignment.setStatus(ShiftAssignmentStatus.SCHEDULED);
        assignment.setPlannedMinutes(resolvePlannedMinutes(request.plannedStart(), request.plannedEnd(), request.plannedMinutes()));

        applyDefaultRates(assignment, shift.getTemplate());
        normalizeFinancialFields(assignment);
        applyAuditMetadata(assignment, true);

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);
        log.info("Created shift assignment {} for shift {}", saved.getId(), shift.getId());
        return shiftAssignmentMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ShiftAssignmentResponseDTO updateAssignment(Long assignmentId, ShiftAssignmentUpdateRequestDTO request) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        ensureAssignmentEditable(assignment);

        LocalTime start = Optional.ofNullable(request.plannedStart()).orElse(assignment.getPlannedStart());
        LocalTime end = Optional.ofNullable(request.plannedEnd()).orElse(assignment.getPlannedEnd());
        validateTimeRange(start, end);
        guardOverlap(assignment.getUser().getId(), assignment.getShift().getShiftDate(), start, end, assignment.getId());

        assignment.setPlannedStart(start);
        assignment.setPlannedEnd(end);
        assignment.setPlannedMinutes(resolvePlannedMinutes(start, end, request.plannedMinutes()));

        if (request.hourlyRate() != null) {
            assignment.setHourlyRate(request.hourlyRate());
        }
        if (request.fixedAllowance() != null) {
            assignment.setFixedAllowance(request.fixedAllowance());
        }
        if (StringUtils.hasText(request.notes())) {
            assignment.setNotes(request.notes());
        }

        normalizeFinancialFields(assignment);
        applyAuditMetadata(assignment, false);

        ShiftAssignment updated = shiftAssignmentRepository.save(assignment);
        recalculateAssignment(updated.getId());
        log.info("Updated shift assignment {}", assignmentId);
        return shiftAssignmentMapper.toResponseDTO(updated);
    }

    @Override
    @Transactional
    public ShiftAssignmentResponseDTO updateStatus(Long assignmentId, ShiftAssignmentStatusUpdateRequestDTO request) {
        ShiftAssignment assignment = findAssignment(assignmentId);

        if (assignment.getStatus() == ShiftAssignmentStatus.COMPLETED
                && request.status() == ShiftAssignmentStatus.CANCELLED) {
            throw new ShiftAssignmentStateException("Cannot cancel a completed assignment");
        }

        assignment.setStatus(request.status());
        if (StringUtils.hasText(request.notes())) {
            assignment.setNotes(request.notes());
        }
        applyAuditMetadata(assignment, false);

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);
        log.info("Updated status for assignment {} to {}", assignmentId, request.status());
        return shiftAssignmentMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAssignment(Long assignmentId) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        if (assignment.getStatus() == ShiftAssignmentStatus.IN_PROGRESS
                || assignment.getStatus() == ShiftAssignmentStatus.COMPLETED) {
            throw new ShiftAssignmentStateException("Cannot delete an in-progress or completed assignment");
        }
        shiftAssignmentRepository.delete(assignment);
        log.info("Deleted shift assignment {}", assignmentId);
    }

    @Override
    @Transactional
    public void recalculateAssignment(Long assignmentId) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        recalculateAttendanceMetrics(assignment);
        recalculateOrderMetrics(assignment);
        recalculatePayrollMetrics(assignment);
        applyAuditMetadata(assignment, false);
        shiftAssignmentRepository.save(assignment);
    }

    @Override
    public List<ShiftAssignmentResponseDTO> getAssignmentsForCurrentUser() {
        Long currentUserId = SecurityUtil.getCurrentUsername()
                .flatMap(username -> userRepository.findByUsername(username).map(User::getId))
                .orElseThrow(UserNotAuthenticatedException::new);

        return shiftAssignmentRepository.findByUserIdOrderByShift_ShiftDateDesc(currentUserId).stream()
                .map(shiftAssignmentMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    private ShiftAssignment findAssignment(Long assignmentId) {
        return shiftAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ShiftAssignmentNotFoundException(assignmentId));
    }

    private ShiftInstance findShift(Long shiftId) {
        return shiftInstanceRepository.findById(shiftId)
                .orElseThrow(() -> new ShiftNotFoundException(shiftId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void ensureShiftIsActive(ShiftInstance shift) {
        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new ShiftAssignmentStateException("Cannot assign employee to a cancelled shift");
        }
    }

    private void ensureAssignmentEditable(ShiftAssignment assignment) {
        ShiftStatus shiftStatus = assignment.getShift().getStatus();
        if (shiftStatus == ShiftStatus.LOCKED || shiftStatus == ShiftStatus.DONE) {
            throw new ShiftAssignmentLockedException(assignment.getShift().getId());
        }
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new ShiftAssignmentStateException("Start and end time must be provided");
        }
        if (!start.isBefore(end)) {
            throw new ShiftAssignmentStateException("Start time must be before end time");
        }
    }

    private void guardOverlap(Long userId,
                              LocalDate shiftDate,
                              LocalTime start,
                              LocalTime end,
                              Long excludeAssignmentId) {
        boolean overlapping = excludeAssignmentId == null
                ? shiftAssignmentRepository.hasOverlappingAssignment(userId, shiftDate, start, end)
                : shiftAssignmentRepository.hasOverlappingAssignmentExcludingId(excludeAssignmentId, userId, shiftDate, start, end);
        if (Boolean.TRUE.equals(overlapping)) {
            throw new ShiftAssignmentOverlapException();
        }
    }

    private int resolvePlannedMinutes(LocalTime start, LocalTime end, Integer overrideMinutes) {
        if (overrideMinutes != null && overrideMinutes > 0) {
            return overrideMinutes;
        }
        return (int) Duration.between(start, end).toMinutes();
    }

    private void applyDefaultRates(ShiftAssignment assignment, ShiftTemplate template) {
        if (template == null) {
            return;
        }
        if (assignment.getHourlyRate() == null) {
            assignment.setHourlyRate(template.getDefaultHourlyRate());
        }
        if (assignment.getFixedAllowance() == null) {
            assignment.setFixedAllowance(template.getDefaultFixedAllowance());
        }
    }

    private void normalizeFinancialFields(ShiftAssignment assignment) {
        assignment.setHourlyRate(Optional.ofNullable(assignment.getHourlyRate()).orElse(BigDecimal.ZERO));
        assignment.setFixedAllowance(Optional.ofNullable(assignment.getFixedAllowance()).orElse(BigDecimal.ZERO));
        assignment.setTotalRevenue(Optional.ofNullable(assignment.getTotalRevenue()).orElse(BigDecimal.ZERO));
        assignment.setBonusAmount(Optional.ofNullable(assignment.getBonusAmount()).orElse(BigDecimal.ZERO));
        assignment.setPenaltyAmount(Optional.ofNullable(assignment.getPenaltyAmount()).orElse(BigDecimal.ZERO));
        assignment.setBasePayroll(Optional.ofNullable(assignment.getBasePayroll()).orElse(BigDecimal.ZERO));
        assignment.setAdjustmentTotal(Optional.ofNullable(assignment.getAdjustmentTotal()).orElse(BigDecimal.ZERO));
        assignment.setCalculatedPayroll(Optional.ofNullable(assignment.getCalculatedPayroll()).orElse(BigDecimal.ZERO));
    }

    private void recalculateAttendanceMetrics(ShiftAssignment assignment) {
        List<AttendanceRecord> records = attendanceRecordRepository.findByAssignmentId(assignment.getId());
        int totalMinutes = records.stream()
                .filter(record -> record.getCheckInAt() != null && record.getCheckOutAt() != null)
                .mapToInt(record -> (int) Duration.between(record.getCheckInAt(), record.getCheckOutAt()).toMinutes())
                .filter(value -> value > 0)
                .sum();
        assignment.setActualMinutes(totalMinutes);
    }

    private void recalculateOrderMetrics(ShiftAssignment assignment) {
        if (assignment.getUser() == null) {
            assignment.setTotalOrders(0);
            assignment.setTotalRevenue(BigDecimal.ZERO);
            return;
        }

        LocalDateTime rangeStart = combine(assignment.getShift().getShiftDate(), assignment.getPlannedStart());
        LocalDateTime rangeEnd = combineEnd(assignment.getShift().getShiftDate(), assignment.getPlannedStart(), assignment.getPlannedEnd());

        List<Order> orders = orderRepository.findOrdersForStaffBetween(
                assignment.getUser().getId(), OrderStatus.PAID, rangeStart, rangeEnd);

        assignment.setTotalOrders(orders.size());
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assignment.setTotalRevenue(totalRevenue);
    }

    private void recalculatePayrollMetrics(ShiftAssignment assignment) {
        BigDecimal hourlyRate = Optional.ofNullable(assignment.getHourlyRate()).orElse(BigDecimal.ZERO);
        BigDecimal fixedAllowance = Optional.ofNullable(assignment.getFixedAllowance()).orElse(BigDecimal.ZERO);
        BigDecimal hoursWorked = BigDecimal.valueOf(assignment.getActualMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal basePayroll = hourlyRate.multiply(hoursWorked).add(fixedAllowance);

        List<ShiftPerformanceAdjustment> adjustments = adjustmentRepository.findByAssignmentId(assignment.getId());
        BigDecimal bonus = sumAdjustments(adjustments, AdjustmentType.BONUS);
        BigDecimal penalty = sumAdjustments(adjustments, AdjustmentType.PENALTY);
        BigDecimal adjustmentTotal = bonus.subtract(penalty);

        assignment.setBasePayroll(basePayroll);
        assignment.setBonusAmount(bonus);
        assignment.setPenaltyAmount(penalty);
        assignment.setAdjustmentTotal(adjustmentTotal);
        assignment.setCalculatedPayroll(basePayroll.add(adjustmentTotal));
    }

    private BigDecimal sumAdjustments(List<ShiftPerformanceAdjustment> adjustments, AdjustmentType type) {
        return adjustments.stream()
                .filter(adj -> !adj.isRevoked() && adj.getType() == type)
                .map(ShiftPerformanceAdjustment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void applyAuditMetadata(ShiftAssignment assignment, boolean isNew) {
        String actor = SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
        if (isNew) {
            assignment.setCreatedBy(actor);
        }
        assignment.setUpdatedBy(actor);
    }

    private LocalDateTime combine(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time);
    }

    private LocalDateTime combineEnd(LocalDate date, LocalTime start, LocalTime end) {
        LocalDateTime endDateTime = LocalDateTime.of(date, end);
        if (!end.isAfter(start)) {
            endDateTime = endDateTime.plusDays(1);
        }
        return endDateTime;
    }
}
