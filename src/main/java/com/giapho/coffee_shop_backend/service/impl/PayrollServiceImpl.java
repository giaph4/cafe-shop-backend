package com.giapho.coffee_shop_backend.service.impl;

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
import com.giapho.coffee_shop_backend.exception.shift.PayrollCycleNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.PayrollCycleValidationException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentNotFoundException;
import com.giapho.coffee_shop_backend.service.PayrollService;
import com.giapho.coffee_shop_backend.service.LockManager;
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ConcurrentModificationException;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollServiceImpl implements PayrollService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final PayrollCycleRepository cycleRepository;
    private final PayrollSummaryRepository summaryRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ShiftAssignmentService shiftAssignmentService;
    private final LockManager lockManager;

    @Override
    public PayrollCycleResponseDTO createCycle(PayrollCycleRequestDTO request) {
        validateCycleDates(request.startDate(), request.endDate());
        ensureCodeIsUnique(request.code(), null);
        String actor = resolveActor();

        PayrollCycle cycle = PayrollCycle.builder()
                .code(request.code())
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(Optional.ofNullable(request.status()).orElse(PayrollCycleStatus.DRAFT))
                .notes(request.notes())
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        if (cycle.getStatus() == PayrollCycleStatus.APPROVED) {
            LocalDateTime now = LocalDateTime.now();
            cycle.setApprovedBy(actor);
            cycle.setApprovedAt(now);
        }

        PayrollCycle saved = cycleRepository.save(cycle);
        log.info("Created payroll cycle {} with code {}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    public PayrollCycleResponseDTO updateCycle(Long cycleId, PayrollCycleRequestDTO request) {
        PayrollCycle cycle = findCycle(cycleId);
        validateCycleDates(request.startDate(), request.endDate());
        ensureCodeIsUnique(request.code(), cycle);

        cycle.setCode(request.code());
        cycle.setName(request.name());
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());
        cycle.setNotes(request.notes());
        applyApprovalMetadata(cycle, request.status());
        cycle.setUpdatedBy(resolveActor());

        PayrollCycle saved = cycleRepository.save(cycle);
        log.info("Updated payroll cycle {}", cycleId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollCycleResponseDTO getCycle(Long cycleId) {
        return toResponse(findCycle(cycleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollCycleResponseDTO> searchCycles(PayrollCycleStatus status, LocalDate from, LocalDate to) {
        return cycleRepository.search(status, from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<PayrollSummaryDTO> regenerateSummaries(Long cycleId) {
        // Add a lock to prevent concurrent regeneration for the same cycle
        String lockKey = "payroll-regenerate-lock-" + cycleId;
        try {
            // Try to acquire a lock with 5 seconds timeout
            if (!lockManager.acquireLock(lockKey, 5, TimeUnit.SECONDS)) {
                throw new ConcurrentModificationException("Another payroll regeneration is in progress for cycle: " + cycleId);
            }

            PayrollCycle cycle = findCycle(cycleId);
            LocalDate start = cycle.getStartDate();
            LocalDate end = cycle.getEndDate();

            log.info("Regenerating payroll summaries for cycle {} ({} - {})", cycleId, start, end);

            // First, get all assignments for the cycle
            List<ShiftAssignment> assignmentsInRange = shiftAssignmentRepository.findByShift_ShiftDateBetween(start, end);
            if (assignmentsInRange.isEmpty()) {
                summaryRepository.deleteByCycleId(cycleId);
                return List.of();
            }

            // Recalculate all assignments to ensure they're up to date
            assignmentsInRange.stream()
                    .map(ShiftAssignment::getId)
                    .distinct()
                    .forEach(id -> {
                        try {
                            shiftAssignmentService.recalculateAssignment(id);
                        } catch (ShiftAssignmentNotFoundException ex) {
                            log.warn("Skip recalculation due to missing assignment {}", id);
                        }
                    });

            // Get fresh data after recalculation
            List<ShiftAssignment> refreshedAssignments = shiftAssignmentRepository.findByShift_ShiftDateBetween(start, end);
            Map<User, List<ShiftAssignment>> assignmentsByUser = refreshedAssignments.stream()
                    .filter(assignment -> assignment.getUser() != null)
                    .filter(assignment -> assignment.getStatus() != ShiftAssignmentStatus.CANCELLED)
                    .collect(Collectors.groupingBy(ShiftAssignment::getUser));

            // Delete existing summaries in a batch
            summaryRepository.deleteByCycleId(cycleId);
            
            // Flush to ensure deletes are applied before inserts
            summaryRepository.flush();

            // Build and save new summaries
            List<PayrollSummaryDTO> summaries = new ArrayList<>();
            for (Map.Entry<User, List<ShiftAssignment>> entry : assignmentsByUser.entrySet()) {
                try {
                    PayrollSummary summary = buildSummaryForUser(cycle, entry.getKey(), entry.getValue(), start, end);
                    PayrollSummary saved = summaryRepository.save(summary);
                    summaries.add(toSummaryDto(saved));
                } catch (DataIntegrityViolationException e) {
                    // If we hit a duplicate key, log it and continue with the next user
                    log.warn("Skipping duplicate payroll summary for user {} in cycle {}", 
                            entry.getKey().getId(), cycleId, e);
                }
            }

            summaries.sort(Comparator
                    .comparing(PayrollSummaryDTO::getFullName, Comparator.nullsLast(String::compareTo))
                    .thenComparing(PayrollSummaryDTO::getUsername, Comparator.nullsLast(String::compareTo)));

            return summaries;
        } finally {
            // Always release the lock
            lockManager.releaseLock(lockKey);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayrollSummaryDTO> getSummaries(Long cycleId, Long userId) {
        return summaryRepository.search(cycleId, userId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    private void ensureCodeIsUnique(String code, PayrollCycle current) {
        Optional<PayrollCycle> existing = cycleRepository.findByCode(code);
        if (existing.isEmpty()) {
            return;
        }
        if (current != null && Objects.equals(existing.get().getId(), current.getId())) {
            return;
        }
        throw new PayrollCycleValidationException("Mã chu kỳ lương đã tồn tại");
    }

    private void validateCycleDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new PayrollCycleValidationException("Ngày bắt đầu và kết thúc chu kỳ không được để trống");
        }
        if (end.isBefore(start)) {
            throw new PayrollCycleValidationException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }
    }

    private void applyApprovalMetadata(PayrollCycle cycle, PayrollCycleStatus newStatus) {
        if (newStatus == null) {
            return;
        }
        PayrollCycleStatus previousStatus = cycle.getStatus();
        cycle.setStatus(newStatus);
        String actor = resolveActor();
        if (newStatus == PayrollCycleStatus.APPROVED) {
            cycle.setApprovedBy(actor);
            cycle.setApprovedAt(LocalDateTime.now());
        } else if (previousStatus == PayrollCycleStatus.APPROVED && newStatus != PayrollCycleStatus.APPROVED) {
            cycle.setApprovedBy(null);
            cycle.setApprovedAt(null);
        }
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

    private PayrollCycleResponseDTO toResponse(PayrollCycle cycle) {
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

    private PayrollSummaryDTO toSummaryDto(PayrollSummary summary) {
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
                .orElseThrow(() -> new PayrollCycleNotFoundException(cycleId));
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
