package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.ShiftPerformanceAdjustment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.AdjustmentType;
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
import com.giapho.coffee_shop_backend.mapper.ShiftAssignmentMapper;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftAssignmentService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftInstanceRepository shiftInstanceRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ShiftPerformanceAdjustmentRepository adjustmentRepository;
    private final ShiftAssignmentMapper shiftAssignmentMapper;

    @Transactional(readOnly = true)
    public ShiftAssignmentResponseDTO getAssignment(Long assignmentId) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        return shiftAssignmentMapper.toResponseDTO(assignment);
    }

    @Transactional(readOnly = true)
    public List<ShiftAssignmentResponseDTO> getAssignmentsForShift(Long shiftId) {
        return shiftAssignmentRepository.findByShiftId(shiftId).stream()
                .map(shiftAssignmentMapper::toResponseDTO)
                .toList();
    }

    public ShiftAssignmentResponseDTO createAssignment(ShiftAssignmentRequestDTO request) {
        ShiftInstance shift = shiftInstanceRepository.findById(request.shiftId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca với ID: " + request.shiftId()));

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new IllegalStateException("Không thể phân công nhân viên vào ca đã bị hủy");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với ID: " + request.userId()));

        validateTimeRange(request.plannedStart(), request.plannedEnd());
        ensureNoOverlap(user.getId(), shift.getShiftDate(), request.plannedStart(), request.plannedEnd());

        ShiftAssignment assignment = shiftAssignmentMapper.toEntity(request);
        assignment.setShift(shift);
        assignment.setUser(user);
        assignment.setPlannedMinutes(resolvePlannedMinutes(request.plannedStart(), request.plannedEnd(), request.plannedMinutes()));

        applyDefaultRates(assignment, shift.getTemplate());
        normalizeNumericFields(assignment);

        String actor = SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
        assignment.setCreatedBy(actor);
        assignment.setUpdatedBy(actor);

        ShiftAssignment saved = shiftAssignmentRepository.save(assignment);
        log.info("Created shift assignment {} for shift {}", saved.getId(), shift.getId());
        return shiftAssignmentMapper.toResponseDTO(saved);
    }

    public ShiftAssignmentResponseDTO updateAssignment(Long assignmentId, ShiftAssignmentUpdateRequestDTO request) {
        ShiftAssignment assignment = findAssignment(assignmentId);

        if (assignment.getShift().getStatus() == ShiftStatus.LOCKED || assignment.getShift().getStatus() == ShiftStatus.DONE) {
            throw new IllegalStateException("Không thể cập nhật phân công khi ca đã khóa hoặc hoàn thành");
        }

        LocalTime newStart = Optional.ofNullable(request.plannedStart()).orElse(assignment.getPlannedStart());
        LocalTime newEnd = Optional.ofNullable(request.plannedEnd()).orElse(assignment.getPlannedEnd());
        validateTimeRange(newStart, newEnd);
        ensureNoOverlapExcludingCurrent(assignment, newStart, newEnd);

        assignment.setPlannedStart(newStart);
        assignment.setPlannedEnd(newEnd);
        assignment.setPlannedMinutes(resolvePlannedMinutes(newStart, newEnd, request.plannedMinutes()));

        if (request.hourlyRate() != null) {
            assignment.setHourlyRate(request.hourlyRate());
        }
        if (request.fixedAllowance() != null) {
            assignment.setFixedAllowance(request.fixedAllowance());
        }
        if (request.notes() != null) {
            assignment.setNotes(request.notes());
        }

        assignment.setUpdatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));

        normalizeNumericFields(assignment);
        ShiftAssignment updated = shiftAssignmentRepository.save(assignment);
        recalculateAssignment(updated.getId());
        return shiftAssignmentMapper.toResponseDTO(updated);
    }

    public ShiftAssignmentResponseDTO updateStatus(Long assignmentId, ShiftAssignmentStatusUpdateRequestDTO request) {
        ShiftAssignment assignment = findAssignment(assignmentId);

        if (request.status() == ShiftAssignmentStatus.CANCELLED && assignment.getStatus() == ShiftAssignmentStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy phân công đã hoàn thành");
        }

        assignment.setStatus(request.status());
        if (request.notes() != null) {
            assignment.setNotes(request.notes());
        }
        assignment.setUpdatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));

        ShiftAssignment updated = shiftAssignmentRepository.save(assignment);
        return shiftAssignmentMapper.toResponseDTO(updated);
    }

    public void deleteAssignment(Long assignmentId) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        if (assignment.getStatus() == ShiftAssignmentStatus.IN_PROGRESS || assignment.getStatus() == ShiftAssignmentStatus.COMPLETED) {
            throw new IllegalStateException("Không thể xóa phân công đang thực hiện hoặc đã hoàn thành");
        }
        shiftAssignmentRepository.delete(assignment);
    }

    public void recalculateAssignment(Long assignmentId) {
        ShiftAssignment assignment = findAssignment(assignmentId);
        recalculateAttendanceMetrics(assignment);
        recalculateOrderMetrics(assignment);
        recalculatePayrollMetrics(assignment);
        assignment.setUpdatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));
        shiftAssignmentRepository.save(assignment);
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
        LocalDate shiftDate = assignment.getShift().getShiftDate();
        LocalDateTime start = combine(shiftDate, assignment.getPlannedStart());
        LocalDateTime end = combineEnd(shiftDate, assignment.getPlannedStart(), assignment.getPlannedEnd());

        List<Order> orders = orderRepository.findPaidOrdersForStaffBetween(
                assignment.getUser().getId(),
                start,
                end
        );

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
        BigDecimal bonus = adjustments.stream()
                .filter(adj -> !adj.isRevoked() && adj.getType() == AdjustmentType.BONUS)
                .map(ShiftPerformanceAdjustment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal penalty = adjustments.stream()
                .filter(adj -> !adj.isRevoked() && adj.getType() == AdjustmentType.PENALTY)
                .map(ShiftPerformanceAdjustment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adjustmentTotal = bonus.subtract(penalty);

        assignment.setBasePayroll(basePayroll);
        assignment.setBonusAmount(bonus);
        assignment.setPenaltyAmount(penalty);
        assignment.setAdjustmentTotal(adjustmentTotal);
        assignment.setCalculatedPayroll(basePayroll.add(adjustmentTotal));
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

    private void normalizeNumericFields(ShiftAssignment assignment) {
        assignment.setHourlyRate(Optional.ofNullable(assignment.getHourlyRate()).orElse(BigDecimal.ZERO));
        assignment.setFixedAllowance(Optional.ofNullable(assignment.getFixedAllowance()).orElse(BigDecimal.ZERO));
        assignment.setTotalRevenue(Optional.ofNullable(assignment.getTotalRevenue()).orElse(BigDecimal.ZERO));
        assignment.setBonusAmount(Optional.ofNullable(assignment.getBonusAmount()).orElse(BigDecimal.ZERO));
        assignment.setPenaltyAmount(Optional.ofNullable(assignment.getPenaltyAmount()).orElse(BigDecimal.ZERO));
        assignment.setBasePayroll(Optional.ofNullable(assignment.getBasePayroll()).orElse(BigDecimal.ZERO));
        assignment.setAdjustmentTotal(Optional.ofNullable(assignment.getAdjustmentTotal()).orElse(BigDecimal.ZERO));
        assignment.setCalculatedPayroll(Optional.ofNullable(assignment.getCalculatedPayroll()).orElse(BigDecimal.ZERO));
    }

    private void ensureNoOverlap(Long userId, LocalDate shiftDate, LocalTime start, LocalTime end) {
        if (Boolean.TRUE.equals(shiftAssignmentRepository.hasOverlappingAssignment(userId, shiftDate, start, end))) {
            throw new IllegalArgumentException("Nhân viên đã có ca khác trong khoảng thời gian này");
        }
    }

    private void ensureNoOverlapExcludingCurrent(ShiftAssignment assignment, LocalTime start, LocalTime end) {
        if (Boolean.TRUE.equals(shiftAssignmentRepository.hasOverlappingAssignmentExcludingId(
                assignment.getId(), assignment.getUser().getId(), assignment.getShift().getShiftDate(), start, end
        ))) {
            throw new IllegalArgumentException("Nhân viên đã có ca khác trong khoảng thời gian này");
        }
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc không được để trống");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }
    }

    private int resolvePlannedMinutes(LocalTime start, LocalTime end, Integer override) {
        if (override != null && override > 0) {
            return override;
        }
        return (int) Duration.between(start, end).toMinutes();
    }

    private ShiftAssignment findAssignment(Long assignmentId) {
        return shiftAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phân công với ID: " + assignmentId));
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
