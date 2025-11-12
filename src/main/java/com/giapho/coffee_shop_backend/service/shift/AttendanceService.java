package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.enums.AttendanceSource;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceCheckRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceRecordResponseDTO;
import com.giapho.coffee_shop_backend.mapper.AttendanceRecordMapper;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UserRepository userRepository;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ShiftAssignmentService shiftAssignmentService;

    public AttendanceRecordResponseDTO checkIn(AttendanceCheckRequestDTO request) {
        ShiftAssignment assignment = resolveAssignment(request);
        validateShiftState(assignment.getShift());
        validateAssignmentStateForAttendance(assignment);

        if (attendanceRecordRepository.findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(assignment.getId()).isPresent()) {
            throw new IllegalStateException("Nhân viên đã check-in và chưa check-out");
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setAssignment(assignment);
        record.setCheckInAt(LocalDateTime.now());
        record.setSource(defaultSource(request.source()));
        record.setNote(request.note());

        long lateMinutes = calculateLateMinutes(record.getCheckInAt(), assignment);
        record.setLateMinutes((int) lateMinutes);
        record.setCreatedBy(resolveActor());
        record.setUpdatedBy(record.getCreatedBy());

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        shiftAssignmentService.recalculateAssignment(assignment.getId());
        log.info("Check-in assignment {} at {}", assignment.getId(), saved.getCheckInAt());
        return attendanceRecordMapper.toResponseDTO(saved);
    }

    public AttendanceRecordResponseDTO checkOut(AttendanceCheckRequestDTO request) {
        ShiftAssignment assignment = resolveAssignment(request);
        validateShiftState(assignment.getShift());
        validateAssignmentStateForAttendance(assignment);

        AttendanceRecord record = attendanceRecordRepository
                .findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(assignment.getId())
                .orElseThrow(() -> new IllegalStateException("Không có phiên check-in đang mở để check-out"));

        record.setCheckOutAt(LocalDateTime.now());
        record.setSource(defaultSource(request.source()));
        record.setNote(mergeNotes(record.getNote(), request.note()));

        long earlyLeave = calculateEarlyLeaveMinutes(record.getCheckOutAt(), assignment);
        record.setEarlyLeaveMinutes((int) earlyLeave);
        record.setUpdatedBy(resolveActor());

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        shiftAssignmentService.recalculateAssignment(assignment.getId());
        log.info("Check-out assignment {} at {}", assignment.getId(), saved.getCheckOutAt());
        return attendanceRecordMapper.toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getAttendanceForAssignment(Long assignmentId) {
        return attendanceRecordRepository.findByAssignmentId(assignmentId).stream()
                .map(attendanceRecordMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponseDTO> getAttendanceForShift(Long shiftId) {
        return attendanceRecordRepository.findByShiftId(shiftId).stream()
                .map(attendanceRecordMapper::toResponseDTO)
                .toList();
    }

    private ShiftAssignment resolveAssignment(AttendanceCheckRequestDTO request) {
        Long assignmentId = request.assignmentId();
        if (assignmentId != null) {
            return shiftAssignmentRepository.findById(assignmentId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phân công với ID: " + assignmentId));
        }

        Long userId = request.userId();
        if (userId == null) {
            String username = SecurityUtil.getCurrentUsername()
                    .orElseThrow(() -> new IllegalArgumentException("Không xác định được nhân viên để chấm công"));

            userId = userRepository.findByUsername(username)
                    .map(user -> user.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên với username: " + username));
        }

        if (request.shiftId() == null) {
            throw new IllegalArgumentException("Cần cung cấp shiftId để xác định phân công");
        }

        return shiftAssignmentRepository.findByShiftIdAndUserId(request.shiftId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phân công cho nhân viên và ca đã chọn"));
    }

    private void validateShiftState(ShiftInstance shift) {
        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new IllegalStateException("Ca đã bị hủy, không thể chấm công");
        }
    }

    private void validateAssignmentStateForAttendance(ShiftAssignment assignment) {
        if (assignment.getStatus() == ShiftAssignmentStatus.CANCELLED) {
            throw new IllegalStateException("Phân công đã bị hủy, không thể chấm công");
        }
    }

    private long calculateLateMinutes(LocalDateTime checkIn, ShiftAssignment assignment) {
        LocalDateTime plannedStart = LocalDateTime.of(assignment.getShift().getShiftDate(), assignment.getPlannedStart());
        if (checkIn.isAfter(plannedStart)) {
            return Duration.between(plannedStart, checkIn).toMinutes();
        }
        return 0;
    }

    private long calculateEarlyLeaveMinutes(LocalDateTime checkOut, ShiftAssignment assignment) {
        LocalDateTime plannedEnd = LocalDateTime.of(assignment.getShift().getShiftDate(), assignment.getPlannedEnd());
        if (!assignment.getPlannedEnd().isAfter(assignment.getPlannedStart())) {
            plannedEnd = plannedEnd.plusDays(1);
        }
        if (checkOut.isBefore(plannedEnd)) {
            return Duration.between(checkOut, plannedEnd).toMinutes();
        }
        return 0;
    }

    private AttendanceSource defaultSource(AttendanceSource source) {
        return source != null ? source : AttendanceSource.WEB;
    }

    private String mergeNotes(String existing, String extra) {
        if (extra == null || extra.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return extra;
        }
        return existing + " | " + extra;
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
