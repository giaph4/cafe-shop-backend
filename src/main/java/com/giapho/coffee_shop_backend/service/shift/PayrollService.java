package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.PayrollCycle;
import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.PayrollCycleStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.PayrollCycleRepository;
import com.giapho.coffee_shop_backend.domain.repository.PayrollSummaryRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollCycleResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.PayrollSummaryDTO;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final PayrollCycleRepository cycleRepository;
    private final PayrollSummaryRepository summaryRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ShiftAssignmentService shiftAssignmentService;

    public PayrollCycleResponseDTO createCycle(PayrollCycleRequestDTO request) {
        validateCycleDates(request.startDate(), request.endDate());
        cycleRepository.findByCode(request.code()).ifPresent(existing -> {
            throw new IllegalArgumentException("Mã chu kỳ lương đã tồn tại");
        });

        String actor = resolveActor();
        LocalDateTime now = LocalDateTime.now();
        PayrollCycle cycle = PayrollCycle.builder()
                .code(request.code())
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(Optional.ofNullable(request.status()).orElse(PayrollCycleStatus.DRAFT))
                .notes(request.notes())
                .createdBy(actor)
                .updatedBy(actor)
                .approvedBy(null)
                .approvedAt(null)
                .build();

        if (cycle.getStatus() == PayrollCycleStatus.APPROVED) {
            cycle.setApprovedBy(actor);
            cycle.setApprovedAt(now);
        }

        PayrollCycle saved = cycleRepository.save(cycle);
        log.info("Created payroll cycle {} ({} - {})", saved.getId(), saved.getStartDate(), saved.getEndDate());
        return toCycleResponse(saved);
    }

    public PayrollCycleResponseDTO updateCycle(Long cycleId, PayrollCycleRequestDTO request) {
        PayrollCycle cycle = findCycle(cycleId);
        validateCycleDates(request.startDate(), request.endDate());

        if (!Objects.equals(cycle.getCode(), request.code())) {
            cycleRepository.findByCode(request.code()).ifPresent(existing -> {
                if (!Objects.equals(existing.getId(), cycleId)) {
                    throw new IllegalArgumentException("Mã chu kỳ lương đã tồn tại");
                }
            });
            cycle.setCode(request.code());
        }

        cycle.setName(request.name());
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());
        if (request.status() != null) {
            PayrollCycleStatus previousStatus = cycle.getStatus();
            PayrollCycleStatus newStatus = request.status();
            cycle.setStatus(newStatus);
            if (newStatus == PayrollCycleStatus.APPROVED) {
                cycle.setApprovedBy(resolveActor());
                cycle.setApprovedAt(LocalDateTime.now());
            } else if (previousStatus == PayrollCycleStatus.APPROVED && newStatus != PayrollCycleStatus.APPROVED) {
                cycle.setApprovedBy(null);
                cycle.setApprovedAt(null);
            }
        }
        cycle.setNotes(request.notes());
        cycle.setUpdatedBy(resolveActor());

        PayrollCycle saved = cycleRepository.save(cycle);
        log.info("Updated payroll cycle {}", cycleId);
        return toCycleResponse(saved);
    }

    @Transactional(readOnly = true)
    public PayrollCycleResponseDTO getCycle(Long cycleId) {
        return toCycleResponse(findCycle(cycleId));
    }

    @Transactional(readOnly = true)
    public List<PayrollCycleResponseDTO> searchCycles(PayrollCycleStatus status, LocalDate from, LocalDate to) {
        return cycleRepository.search(status, from, to).stream()
                .map(this::toCycleResponse)
                .toList();
    }

    public List<PayrollSummaryDTO> regenerateSummaries(Long cycleId) {
        PayrollCycle cycle = findCycle(cycleId);
        LocalDate start = cycle.getStartDate();
        LocalDate end = cycle.getEndDate();

        log.info("Regenerating payroll summaries for cycle {} ({} - {})", cycleId, start, end);

        List<ShiftAssignment> assignmentsInRange = shiftAssignmentRepository.findByShift_ShiftDateBetween(start, end);
        if (assignmentsInRange.isEmpty()) {
            summaryRepository.deleteByCycleId(cycleId);
            return List.of();
        }

        assignmentsInRange.stream()
                .map(ShiftAssignment::getId)
                .forEach(shiftAssignmentService::recalculateAssignment);

        List<ShiftAssignment> refreshedAssignments = shiftAssignmentRepository.findByShift_ShiftDateBetween(start, end);

        Map<User, List<ShiftAssignment>> assignmentsByUser = refreshedAssignments.stream()
                .filter(assignment -> assignment.getUser() != null)
                .filter(assignment -> assignment.getStatus() != ShiftAssignmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(ShiftAssignment::getUser));

        summaryRepository.deleteByCycleId(cycleId);

        List<PayrollSummaryDTO> summaries = new ArrayList<>();

        for (Map.Entry<User, List<ShiftAssignment>> entry : assignmentsByUser.entrySet()) {
            User user = entry.getKey();
            List<ShiftAssignment> userAssignments = entry.getValue();

            PayrollSummary summary = buildSummaryForUser(cycle, user, userAssignments, start, end);
            PayrollSummary saved = summaryRepository.save(summary);
            summaries.add(toDto(saved));
        }

        summaries.sort(Comparator
                .comparing(PayrollSummaryDTO::getFullName, Comparator.nullsLast(String::compareTo))
                .thenComparing(PayrollSummaryDTO::getUsername, Comparator.nullsLast(String::compareTo)));

        return summaries;
    }

    @Transactional(readOnly = true)
    public List<PayrollSummaryDTO> getSummaries(Long cycleId, Long userId) {
        List<PayrollSummary> summaries = summaryRepository.search(cycleId, userId);
        return summaries.stream()
                .map(this::toDto)
                .toList();
    }

    private PayrollSummary buildSummaryForUser(PayrollCycle cycle,
                                               User user,
                                               List<ShiftAssignment> assignments,
                                               LocalDate start,
                                               LocalDate end) {
        String actor = resolveActor();

        int assignmentCount = assignments.size();
        int attendanceCount = attendanceRecordRepository
                .findRecordsForUserBetweenDates(user.getId(), start, end)
                .size();

        int totalActualMinutes = assignments.stream()
                .map(ShiftAssignment::getActualMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int totalOrders = assignments.stream()
                .map(ShiftAssignment::getTotalOrders)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        BigDecimal totalRevenue = assignments.stream()
                .map(ShiftAssignment::getTotalRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBase = assignments.stream()
                .map(ShiftAssignment::getBasePayroll)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBonus = assignments.stream()
                .map(ShiftAssignment::getBonusAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPenalty = assignments.stream()
                .map(ShiftAssignment::getPenaltyAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAdjustment = assignments.stream()
                .map(ShiftAssignment::getAdjustmentTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = assignments.stream()
                .map(ShiftAssignment::getCalculatedPayroll)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PayrollSummary.builder()
                .cycle(cycle)
                .user(user)
                .assignmentCount(assignmentCount)
                .attendanceCount(attendanceCount)
                .totalActualMinutes(totalActualMinutes)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalBasePayroll(totalBase)
                .totalBonus(totalBonus)
                .totalPenalty(totalPenalty)
                .totalAdjustment(totalAdjustment)
                .totalNetPayroll(totalNet)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
    }

    private PayrollCycleResponseDTO toCycleResponse(PayrollCycle cycle) {
        return PayrollCycleResponseDTO.builder()
                .id(cycle.getId())
                .code(cycle.getCode())
                .name(cycle.getName())
                .startDate(cycle.getStartDate())
                .endDate(cycle.getEndDate())
                .status(cycle.getStatus())
                .notes(cycle.getNotes())
                .approvedBy(cycle.getApprovedBy())
                .approvedAt(cycle.getApprovedAt())
                .createdBy(cycle.getCreatedBy())
                .updatedBy(cycle.getUpdatedBy())
                .createdAt(cycle.getCreatedAt())
                .updatedAt(cycle.getUpdatedAt())
                .build();
    }

    private PayrollSummaryDTO toDto(PayrollSummary summary) {
        return PayrollSummaryDTO.builder()
                .cycleId(summary.getCycle().getId())
                .cycleCode(summary.getCycle().getCode())
                .cycleName(summary.getCycle().getName())
                .cycleStartDate(summary.getCycle().getStartDate())
                .cycleEndDate(summary.getCycle().getEndDate())
                .userId(summary.getUser().getId())
                .username(summary.getUser().getUsername())
                .fullName(summary.getUser().getFullName())
                .assignmentCount(summary.getAssignmentCount())
                .attendanceCount(summary.getAttendanceCount())
                .totalActualMinutes(summary.getTotalActualMinutes())
                .totalOrders(summary.getTotalOrders())
                .totalRevenue(summary.getTotalRevenue())
                .totalBasePayroll(summary.getTotalBasePayroll())
                .totalBonus(summary.getTotalBonus())
                .totalPenalty(summary.getTotalPenalty())
                .totalAdjustment(summary.getTotalAdjustment())
                .totalNetPayroll(summary.getTotalNetPayroll())
                .notes(summary.getNotes())
                .build();
    }

    private PayrollCycle findCycle(Long cycleId) {
        return cycleRepository.findById(cycleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chu kỳ lương với ID: " + cycleId));
    }

    private void validateCycleDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc chu kỳ không được để trống");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
