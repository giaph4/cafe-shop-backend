package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.domain.entity.PayrollCycle;
import com.giapho.coffee_shop_backend.domain.entity.PayrollSummary;
import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
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
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import com.giapho.coffee_shop_backend.service.impl.PayrollServiceImpl;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollCycleRepository cycleRepository;
    @Mock
    private PayrollSummaryRepository summaryRepository;
    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private ShiftAssignmentService shiftAssignmentService;

    @InjectMocks
    private PayrollServiceImpl payrollService;

    private MockedStatic<com.giapho.coffee_shop_backend.util.SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = Mockito.mockStatic(com.giapho.coffee_shop_backend.util.SecurityUtil.class);
        securityUtilMock.when(com.giapho.coffee_shop_backend.util.SecurityUtil::getCurrentUsername)
                .thenReturn(Optional.of("manager01"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "manager01",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
        ));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        securityUtilMock.close();
    }

    @Test
    void createCycle_ShouldPersistAndReturnResponse() {
        PayrollCycleRequestDTO request = new PayrollCycleRequestDTO(null, "JAN_2025", "January 2025",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), PayrollCycleStatus.DRAFT, "Ghi chú");

        when(cycleRepository.findByCode("JAN_2025")).thenReturn(Optional.empty());
        when(cycleRepository.save(any(PayrollCycle.class))).thenAnswer(invocation -> {
            PayrollCycle cycle = invocation.getArgument(0);
            cycle.setId(10L);
            cycle.setCreatedAt(LocalDateTime.now());
            cycle.setUpdatedAt(LocalDateTime.now());
            return cycle;
        });

        PayrollCycleResponseDTO responseDTO = payrollService.createCycle(request);

        assertThat(responseDTO.getId()).isEqualTo(10L);
        assertThat(responseDTO.getCode()).isEqualTo("JAN_2025");
        assertThat(responseDTO.getStatus()).isEqualTo(PayrollCycleStatus.DRAFT);
        assertThat(responseDTO.getCreatedBy()).isEqualTo("manager01");
        verify(cycleRepository).save(any(PayrollCycle.class));
    }

    @Test
    void createCycle_ShouldThrow_WhenCodeExists() {
        PayrollCycleRequestDTO request = new PayrollCycleRequestDTO(null, "JAN_2025", "January 2025",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), PayrollCycleStatus.DRAFT, null);

        when(cycleRepository.findByCode("JAN_2025")).thenReturn(Optional.of(PayrollCycle.builder().build()));

        assertThatThrownBy(() -> payrollService.createCycle(request))
                .isInstanceOf(PayrollCycleValidationException.class)
                .hasMessageContaining("Mã chu kỳ lương đã tồn tại");
    }

    @Test
    void updateCycle_ShouldHandleApprovalMetadata() {
        PayrollCycle existing = PayrollCycle.builder()
                .id(20L)
                .code("JAN_2025")
                .name("January 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .status(PayrollCycleStatus.DRAFT)
                .createdBy("system")
                .updatedBy("system")
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .build();

        PayrollCycleRequestDTO request = new PayrollCycleRequestDTO(20L, "JAN_2025", "Tháng 1/2025",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), PayrollCycleStatus.APPROVED, "Đã chốt");

        when(cycleRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(cycleRepository.save(any(PayrollCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollCycleResponseDTO response = payrollService.updateCycle(20L, request);

        assertThat(response.getStatus()).isEqualTo(PayrollCycleStatus.APPROVED);
        assertThat(response.getApprovedBy()).isEqualTo("manager01");
        assertThat(response.getApprovedAt()).isNotNull();
        assertThat(response.getUpdatedBy()).isEqualTo("manager01");
        verify(cycleRepository).save(any(PayrollCycle.class));
    }

    @Test
    void regenerateSummaries_ShouldAggregateAssignments() {
        PayrollCycle cycle = PayrollCycle.builder()
                .id(30L)
                .code("JAN_2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 31))
                .status(PayrollCycleStatus.IN_PROGRESS)
                .build();

        User user = User.builder()
                .id(5L)
                .username("staff01")
                .fullName("Nguyễn A")
                .build();

        ShiftInstance shift = ShiftInstance.builder()
                .id(100L)
                .shiftDate(LocalDate.of(2025, 1, 15))
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        ShiftAssignment assignment = ShiftAssignment.builder()
                .id(500L)
                .shift(shift)
                .user(user)
                .status(ShiftAssignmentStatus.COMPLETED)
                .actualMinutes(180)
                .totalOrders(5)
                .totalRevenue(new BigDecimal("750000"))
                .basePayroll(new BigDecimal("350000"))
                .bonusAmount(new BigDecimal("20000"))
                .penaltyAmount(new BigDecimal("5000"))
                .adjustmentTotal(new BigDecimal("15000"))
                .calculatedPayroll(new BigDecimal("365000"))
                .build();

        when(cycleRepository.findById(30L)).thenReturn(Optional.of(cycle));
        when(shiftAssignmentRepository.findByShift_ShiftDateBetween(cycle.getStartDate(), cycle.getEndDate()))
                .thenReturn(List.of(assignment), List.of(assignment));
        doNothing().when(shiftAssignmentService).recalculateAssignment(500L);
        when(attendanceRecordRepository.findRecordsForUserBetweenDates(5L, cycle.getStartDate(), cycle.getEndDate()))
                .thenReturn(List.of(new AttendanceRecord(), new AttendanceRecord()));
        when(summaryRepository.save(any(PayrollSummary.class))).thenAnswer(invocation -> {
            PayrollSummary summary = invocation.getArgument(0);
            summary.setId(900L);
            return summary;
        });

        List<PayrollSummaryDTO> result = payrollService.regenerateSummaries(30L);

        assertThat(result).hasSize(1);
        PayrollSummaryDTO dto = result.get(0);
        assertThat(dto.getUserId()).isEqualTo(5L);
        assertThat(dto.getAssignmentCount()).isEqualTo(1);
        assertThat(dto.getAttendanceCount()).isEqualTo(2);
        assertThat(dto.getTotalActualMinutes()).isEqualTo(180);
        assertThat(dto.getTotalBonus()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(dto.getTotalPenalty()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(dto.getTotalNetPayroll()).isEqualByComparingTo(new BigDecimal("365000"));

        verify(summaryRepository).deleteByCycleId(30L);
        verify(summaryRepository, times(1)).save(any(PayrollSummary.class));
        verify(shiftAssignmentService).recalculateAssignment(500L);
    }

    @Test
    void regenerateSummaries_ShouldClearWhenNoAssignments() {
        PayrollCycle cycle = PayrollCycle.builder()
                .id(31L)
                .code("EMPTY")
                .startDate(LocalDate.of(2025, 2, 1))
                .endDate(LocalDate.of(2025, 2, 28))
                .build();

        when(cycleRepository.findById(31L)).thenReturn(Optional.of(cycle));
        when(shiftAssignmentRepository.findByShift_ShiftDateBetween(cycle.getStartDate(), cycle.getEndDate()))
                .thenReturn(List.of());

        List<PayrollSummaryDTO> result = payrollService.regenerateSummaries(31L);

        assertThat(result).isEmpty();
        verify(summaryRepository).deleteByCycleId(31L);
    }

    @Test
    void getCycle_ShouldThrowWhenNotFound() {
        when(cycleRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> payrollService.getCycle(999L))
                .isInstanceOf(PayrollCycleNotFoundException.class);
    }
}
