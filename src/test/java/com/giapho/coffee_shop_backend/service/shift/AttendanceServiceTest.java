package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.enums.ShiftAssignmentStatus;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.AttendanceRecordRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceCheckRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.AttendanceValidationException;
import com.giapho.coffee_shop_backend.mapper.AttendanceRecordMapper;
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import com.giapho.coffee_shop_backend.service.impl.AttendanceServiceImpl;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AttendanceRecordMapper attendanceRecordMapper;
    @Mock
    private ShiftAssignmentService shiftAssignmentService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private ShiftAssignment assignment;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = Mockito.mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUsername).thenReturn(Optional.of("staff01"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "staff01",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
        ));

        ShiftInstance instance = ShiftInstance.builder()
                .id(10L)
                .shiftDate(LocalDate.of(2025, 1, 15))
                .status(ShiftStatus.PLANNED)
                .build();

        assignment = ShiftAssignment.builder()
                .id(300L)
                .shift(instance)
                .user(User.builder().id(5L).username("staff01").build())
                .plannedStart(LocalTime.of(7, 0))
                .plannedEnd(LocalTime.of(11, 0))
                .status(ShiftAssignmentStatus.SCHEDULED)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        securityUtilMock.close();
    }

    @Test
    void checkIn_ShouldCreateAttendanceRecord() {
        AttendanceCheckRequestDTO request = new AttendanceCheckRequestDTO(10L, 300L, 5L, null, "Note");

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
        given(attendanceRecordRepository.findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(300L)).willReturn(Optional.empty());
        given(attendanceRecordRepository.save(any(AttendanceRecord.class))).willAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(400L);
            return record;
        });
        given(attendanceRecordMapper.toResponseDTO(any(AttendanceRecord.class))).willReturn(null);

        attendanceService.checkIn(request);

        verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
        verify(shiftAssignmentService).recalculateAssignment(300L);
    }

    @Test
    void checkIn_ShouldFailWhenAlreadyCheckedIn() {
        AttendanceCheckRequestDTO request = new AttendanceCheckRequestDTO(10L, 300L, 5L, null, "Note");

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
        given(attendanceRecordRepository.findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(300L))
                .willReturn(Optional.of(new AttendanceRecord()));

        assertThrows(AttendanceValidationException.class, () -> attendanceService.checkIn(request));
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void checkOut_ShouldUpdateExistingRecord() {
        AttendanceCheckRequestDTO request = new AttendanceCheckRequestDTO(10L, 300L, 5L, null, "Extra note");
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(400L)
                .assignment(assignment)
                .checkInAt(LocalDateTime.of(2025, 1, 15, 7, 5))
                .note("Initial note")
                .build();

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
        given(attendanceRecordRepository.findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(300L))
                .willReturn(Optional.of(existing));
        given(attendanceRecordRepository.save(existing)).willReturn(existing);
        given(attendanceRecordMapper.toResponseDTO(existing)).willReturn(null);

        attendanceService.checkOut(request);

        assertThat(existing.getCheckOutAt()).isNotNull();
        assertThat(existing.getNote()).contains("Initial note").contains("Extra note");
        verify(shiftAssignmentService).recalculateAssignment(300L);
    }

    @Test
    void checkOut_ShouldFailWhenNoOpenSession() {
        AttendanceCheckRequestDTO request = new AttendanceCheckRequestDTO(10L, 300L, 5L, null, "Extra note");

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
        given(attendanceRecordRepository.findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(300L))
                .willReturn(Optional.empty());

        assertThrows(AttendanceValidationException.class, () -> attendanceService.checkOut(request));
    }

    @Test
    void resolveAssignment_ShouldUseCurrentUserWhenAssignmentIdMissing() {
        AttendanceCheckRequestDTO request = new AttendanceCheckRequestDTO(10L, null, null, null, null);

        given(shiftAssignmentRepository.findByShiftIdAndUserId(eq(10L), eq(5L))).willReturn(Optional.of(assignment));
        given(userRepository.findByUsername("staff01")).willReturn(Optional.of(User.builder().id(5L).username("staff01").build()));
        given(attendanceRecordRepository.findFirstByAssignmentIdAndCheckOutAtIsNullOrderByCheckInAtDesc(300L)).willReturn(Optional.empty());
        given(attendanceRecordRepository.save(any(AttendanceRecord.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(attendanceRecordMapper.toResponseDTO(any(AttendanceRecord.class))).willReturn(null);

        attendanceService.checkIn(request);

        verify(shiftAssignmentRepository).findByShiftIdAndUserId(10L, 5L);
    }

    @Test
    void checkIn_ShouldThrow_WhenShiftCancelled() {
        attendanceCancelledSetup();
        AttendanceCheckRequestDTO request = new AttendanceCheckRequestDTO(10L, 300L, 5L, null, null);
        assertThrows(AttendanceValidationException.class, () -> attendanceService.checkIn(request));
    }

    private void attendanceCancelledSetup() {
        assignment.getShift().setStatus(ShiftStatus.CANCELLED);
        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
    }
}
