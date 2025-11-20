package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
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
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentUpdateRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentOverlapException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentStateException;
import com.giapho.coffee_shop_backend.mapper.ShiftAssignmentMapper;
import com.giapho.coffee_shop_backend.service.impl.ShiftAssignmentServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShiftAssignmentServiceTest {

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock
    private ShiftInstanceRepository shiftInstanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private ShiftPerformanceAdjustmentRepository adjustmentRepository;
    @Mock
    private ShiftAssignmentMapper shiftAssignmentMapper;

    @InjectMocks
    private ShiftAssignmentServiceImpl shiftAssignmentService;

    private ShiftInstance shiftInstance;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "manager01",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        ));

        shiftInstance = ShiftInstance.builder()
                .id(10L)
                .shiftDate(LocalDate.of(2025, 1, 15))
                .status(ShiftStatus.PLANNED)
                .template(ShiftTemplate.builder()
                        .id(1L)
                        .defaultHourlyRate(BigDecimal.valueOf(35000))
                        .defaultFixedAllowance(BigDecimal.valueOf(50000))
                        .build())
                .build();

        user = User.builder()
                .id(5L)
                .username("staff01")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssignment_ShouldApplyDefaultsAndPersist() {
        ShiftAssignmentRequestDTO request = new ShiftAssignmentRequestDTO(
                10L,
                5L,
                "BARISTA",
                LocalTime.of(7, 0),
                LocalTime.of(11, 0),
                null,
                null,
                null,
                "Pha chế chính"
        );

        ShiftAssignment entity = ShiftAssignment.builder()
                .plannedStart(request.plannedStart())
                .plannedEnd(request.plannedEnd())
                .roleName(request.roleName())
                .build();

        given(shiftInstanceRepository.findById(10L)).willReturn(Optional.of(shiftInstance));
        given(userRepository.findById(5L)).willReturn(Optional.of(user));
        given(shiftAssignmentRepository.hasOverlappingAssignment(eq(5L), eq(shiftInstance.getShiftDate()), any(), any())).willReturn(false);
        given(shiftAssignmentMapper.toEntity(request)).willReturn(entity);
        given(shiftAssignmentRepository.save(any(ShiftAssignment.class))).willAnswer(invocation -> {
            ShiftAssignment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        given(shiftAssignmentMapper.toResponseDTO(any(ShiftAssignment.class))).willReturn(null);

        shiftAssignmentService.createAssignment(request);

        assertThat(entity.getPlannedMinutes()).isEqualTo(240);
        assertThat(entity.getHourlyRate()).isEqualByComparingTo(BigDecimal.valueOf(35000));
        assertThat(entity.getFixedAllowance()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        verify(shiftAssignmentRepository).save(entity);
    }

    @Test
    void createAssignment_ShouldThrow_WhenOverlap() {
        ShiftAssignmentRequestDTO request = new ShiftAssignmentRequestDTO(
                10L, 5L, "BARISTA", LocalTime.of(7, 0), LocalTime.of(11, 0), null, null, null, null
        );

        given(shiftInstanceRepository.findById(10L)).willReturn(Optional.of(shiftInstance));
        given(userRepository.findById(5L)).willReturn(Optional.of(user));
        given(shiftAssignmentRepository.hasOverlappingAssignment(eq(5L), eq(shiftInstance.getShiftDate()), any(), any())).willReturn(true);

        assertThrows(ShiftAssignmentOverlapException.class, () -> shiftAssignmentService.createAssignment(request));
        verify(shiftAssignmentRepository, never()).save(any());
    }

    @Test
    void updateAssignment_ShouldRecalculateMinutesAndRates() {
        ShiftAssignment assignment = buildAssignment();
        ShiftAssignmentUpdateRequestDTO updateRequest = new ShiftAssignmentUpdateRequestDTO(
                300L,
                LocalTime.of(8, 0),
                LocalTime.of(12, 30),
                null,
                BigDecimal.valueOf(40000),
                BigDecimal.valueOf(60000),
                "Update notes"
        );

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
        given(shiftAssignmentRepository.hasOverlappingAssignmentExcludingId(eq(300L), eq(assignment.getUser().getId()), eq(assignment.getShift().getShiftDate()), any(), any())).willReturn(false);
        given(shiftAssignmentRepository.save(any(ShiftAssignment.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(shiftAssignmentMapper.toResponseDTO(any(ShiftAssignment.class))).willReturn(null);

        shiftAssignmentService.updateAssignment(300L, updateRequest);

        assertThat(assignment.getPlannedMinutes()).isEqualTo(270);
        assertThat(assignment.getHourlyRate()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(assignment.getFixedAllowance()).isEqualByComparingTo(BigDecimal.valueOf(60000));
        assertThat(assignment.getNotes()).isEqualTo("Update notes");
        verify(shiftAssignmentRepository, times(2)).save(assignment);
    }

    @Test
    void updateStatus_ShouldRejectCancellingCompleted() {
        ShiftAssignment assignment = buildAssignment();
        assignment.setStatus(ShiftAssignmentStatus.COMPLETED);

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));

        ShiftAssignmentStatusUpdateRequestDTO request = new ShiftAssignmentStatusUpdateRequestDTO(ShiftAssignmentStatus.CANCELLED, "try cancel");
        assertThrows(ShiftAssignmentStateException.class, () -> shiftAssignmentService.updateStatus(300L, request));
    }

    @Test
    void recalculateAssignment_ShouldAggregateAttendanceAndAdjustments() {
        ShiftAssignment assignment = buildAssignment();

        given(shiftAssignmentRepository.findById(300L)).willReturn(Optional.of(assignment));
        given(attendanceRecordRepository.findByAssignmentId(300L)).willReturn(List.of(
                attendance(assignment, 60),
                attendance(assignment, 120)
        ));
        given(orderRepository.findOrdersForStaffBetween(eq(assignment.getUser().getId()), eq(OrderStatus.PAID), any(), any())).willReturn(List.of());
        given(adjustmentRepository.findByAssignmentId(300L)).willReturn(List.of(
                adjustment(assignment, AdjustmentType.BONUS, BigDecimal.valueOf(20000)),
                adjustment(assignment, AdjustmentType.PENALTY, BigDecimal.valueOf(5000))
        ));

        shiftAssignmentService.recalculateAssignment(300L);

        assertThat(assignment.getActualMinutes()).isEqualTo(180);
        assertThat(assignment.getBonusAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(assignment.getPenaltyAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(assignment.getAdjustmentTotal()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        verify(shiftAssignmentRepository).save(assignment);
    }

    @Test
    void getAssignment_ShouldThrow_WhenNotFound() {
        given(shiftAssignmentRepository.findById(999L)).willReturn(Optional.empty());
        assertThrows(ShiftAssignmentNotFoundException.class, () -> shiftAssignmentService.getAssignment(999L));
    }

    private ShiftAssignment buildAssignment() {
        ShiftAssignment assignment = ShiftAssignment.builder()
                .id(300L)
                .shift(shiftInstance)
                .user(user)
                .plannedStart(LocalTime.of(7, 0))
                .plannedEnd(LocalTime.of(11, 0))
                .plannedMinutes(240)
                .status(ShiftAssignmentStatus.SCHEDULED)
                .hourlyRate(BigDecimal.valueOf(35000))
                .fixedAllowance(BigDecimal.valueOf(50000))
                .build();
        return assignment;
    }

    private AttendanceRecord attendance(ShiftAssignment assignment, int minutes) {
        return AttendanceRecord.builder()
                .assignment(assignment)
                .checkInAt(LocalDateTime.of(2025, 1, 15, 7, 0))
                .checkOutAt(LocalDateTime.of(2025, 1, 15, 7, 0).plusMinutes(minutes))
                .build();
    }

    private ShiftPerformanceAdjustment adjustment(ShiftAssignment assignment, AdjustmentType type, BigDecimal amount) {
        return ShiftPerformanceAdjustment.builder()
                .assignment(assignment)
                .type(type)
                .amount(amount)
                .revoked(false)
                .build();
    }
}
