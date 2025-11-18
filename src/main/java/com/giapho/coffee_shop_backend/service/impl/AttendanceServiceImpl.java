package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.AttendanceSource;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceCheckRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceRecordResponseDTO;
import com.giapho.coffee_shop_backend.exception.shift.AttendanceValidationException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftNotFoundException;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import com.giapho.coffee_shop_backend.mapper.AttendanceRecordMapper;
import com.giapho.coffee_shop_backend.service.AdminAiService;
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AdminAiService.AttendanceService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ShiftAssignmentService shiftAssignmentService;

    @Override
    public AttendanceRecordResponseDTO checkIn(AttendanceCheckRequestDTO request) {
        ShiftAssignment assignment = resolveAssignment(request);
        ensureAttendanceAllowed(assignment);
        ensureNoOpenSession(assignment.getId());

        LocalDateTime now = LocalDateTime.now();
        String actor = resolveActor();

        AttendanceRecord record = AttendanceRecord.builder()
                .assignment(assignment)
                .checkInAt(now)
                .source(resolveSource(request.source()))
                .note(request.note())
                .lateMinutes((int) calculateLateMinutes(now, assignment))
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        shiftAssignmentService.recalculateAssignment(assignment.getId());
        log.info("Check-in assignment {} at {}", assignment.getId(), saved.getCheckInAt());
        return attendanceRecordMapper.toResponseDTO(saved);
    }

    @Override
    public AttendanceRecordResponseDTO checkOut(AttendanceCheckRequestDTO request) {
        ShiftAssignment assignment = resolveAssignment(request);
        ensureAttendanceAllowed(assignment);

        AttendanceRecord activeRecord = findActiveSession(assignment.getId());

        LocalDateTime now = LocalDateTime.now();
        String actor = resolveActor();

        activeRecord.setCheckOutAt(now);
        activeRecord.setSource(resolveSource(request.source()));
        activeRecord.setNote(mergeNotes(activeRecord.getNote(), request.note()));
        activeRecord.setEarlyLeaveMinutes((int) calculateEarlyLeaveMinutes(now, assignment));
        activeRecord.setUpdatedBy(actor);

        AttendanceRecord saved = attendanceRecordRepository.save(activeRecord);
        shiftAssignmentService.recalculateAssignment(assignment.getId());
        log.info("Check-out assignment {} at {}", assignment.getId(), saved.getCheckOutAt());
        return attendanceRecordMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getAttendanceForAssignment(Long assignmentId) {
        return attendanceRecordRepository.findByAssignmentId(assignmentId).stream()
                .map(attendanceRecordMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getAttendanceForShift(Long shiftId) {
        return attendanceRecordRepository.findByShiftId(shiftId).stream()
                .map(attendanceRecordMapper::toResponseDTO)
                .toList();
    }

    private ShiftAssignment resolveAssignment(AttendanceCheckRequestDTO request) {
        if (request.assignmentId() != null) {
            return shiftAssignmentRepository.findById(request.assignmentId())
                    .orElseThrow(() -> new ShiftAssignmentNotFoundException(request.assignmentId()));
        }

        Long shiftId = Optional.ofNullable(request.shiftId())
                .orElseThrow(() -> new AttendanceValidationException("ShiftId is required to determine the assignment"));

        User user = resolveUser(request.userId());

        return shiftAssignmentRepository.findByShiftIdAndUserId(shiftId, user.getId())
                .orElseThrow(() -> new AttendanceValidationException("No assignment found for the provided shift and user"));
    }

    private User resolveUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));
        }

        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new AttendanceValidationException("Cannot determine current user for attendance"));

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private void ensureAttendanceAllowed(ShiftAssignment assignment) {
        ShiftInstance shift = Optional.ofNullable(assignment.getShift())
                .orElseThrow(() -> new ShiftNotFoundException(null));

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new AttendanceValidationException("Shift was cancelled and cannot be attended");
        }
        if (assignment.getStatus() == ShiftAssignmentStatus.CANCELLED) {
            throw new AttendanceValidationException("Assignment was cancelled and cannot be attended");
        }
    }

    private void ensureNoOpenSession(Long assignmentId) {
        boolean sessionOpen = attendanceRecordRepository
                .findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(assignmentId)
                .isPresent();
        if (sessionOpen) {
            throw new AttendanceValidationException("Employee already checked in and has not checked out yet");
        }
    }

    private AttendanceRecord findActiveSession(Long assignmentId) {
        return attendanceRecordRepository
                .findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(assignmentId)
                .orElseThrow(() -> new AttendanceValidationException("No active check-in session to check out"));
    }

    private long calculateLateMinutes(LocalDateTime actualCheckIn, ShiftAssignment assignment) {
        LocalDateTime plannedStart = LocalDateTime.of(assignment.getShift().getShiftDate(), assignment.getPlannedStart());
        if (actualCheckIn.isAfter(plannedStart)) {
            return Duration.between(plannedStart, actualCheckIn).toMinutes();
        }
        return 0;
    }

    private long calculateEarlyLeaveMinutes(LocalDateTime actualCheckout, ShiftAssignment assignment) {
        LocalDate shiftDate = assignment.getShift().getShiftDate();
        LocalTime plannedStart = assignment.getPlannedStart();
        LocalTime plannedEnd = assignment.getPlannedEnd();

        LocalDateTime plannedEndDateTime = LocalDateTime.of(shiftDate, plannedEnd);
        if (!plannedEnd.isAfter(plannedStart)) {
            plannedEndDateTime = plannedEndDateTime.plusDays(1);
        }

        if (actualCheckout.isBefore(plannedEndDateTime)) {
            return Duration.between(actualCheckout, plannedEndDateTime).toMinutes();
        }
        return 0;
    }

    private AttendanceSource resolveSource(AttendanceSource source) {
        return source != null ? source : AttendanceSource.WEB;
    }

    private String mergeNotes(String existing, String extra) {
        if (!StringUtils.hasText(extra)) {
            return existing;
        }
        if (!StringUtils.hasText(existing)) {
            return extra;
        }
        return existing + " | " + extra;
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
