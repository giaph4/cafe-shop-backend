package com.giapho.coffee_shop_backend.controller.dashboard;

import com.giapho.coffee_shop_backend.controller.StaffDashboardController;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.service.RoleDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffDashboardControllerTest {

    @Mock
    private RoleDashboardService roleDashboardService;

    @InjectMocks
    private StaffDashboardController controller;

    @Test
    void getCurrentStaffDashboard_shouldReturnDashboardForSelf() {
        StaffDashboardDTO dashboard = sampleStaffDashboard();
        when(roleDashboardService.buildStaffDashboard(null)).thenReturn(dashboard);

        ResponseEntity<StaffDashboardDTO> response = controller.getCurrentStaffDashboard(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dashboard);
        verify(roleDashboardService).buildStaffDashboard(null);
    }

    @Test
    void getStaffDashboardForManager_shouldReturnDashboardForGivenUser() {
        StaffDashboardDTO dashboard = sampleStaffDashboard();
        when(roleDashboardService.buildStaffDashboard(123L)).thenReturn(dashboard);

        ResponseEntity<StaffDashboardDTO> response = controller.getStaffDashboardForManager(123L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dashboard);
        verify(roleDashboardService).buildStaffDashboard(123L);
    }

    @Test
    void controllerShouldExposeExpectedSecurityRules() {
        PreAuthorize classAnnotation = controller.getClass().getAnnotation(PreAuthorize.class);
        assertThat(classAnnotation).isNotNull();
        assertThat(classAnnotation.value()).contains("'STAFF'");
    }

    private StaffDashboardDTO sampleStaffDashboard() {
        return StaffDashboardDTO.builder()
                .shiftSummary(StaffDashboardDTO.PersonalShiftSummary.builder()
                        .shiftsThisWeek(3)
                        .completedShifts(2)
                        .pendingShifts(1)
                        .lateCheckIns(0)
                        .earlyCheckOuts(0)
                        .build())
                .upcomingShifts(Collections.emptyList())
                .performance(StaffDashboardDTO.PerformanceSnapshot.builder()
                        .totalRevenue(BigDecimal.valueOf(900))
                        .totalOrders(30)
                        .averageOrderValue(BigDecimal.valueOf(30))
                        .positiveFeedbacks(5)
                        .negativeFeedbacks(0)
                        .build())
                .attendance(StaffDashboardDTO.AttendanceStatus.builder()
                        .currentlyCheckedIn(false)
                        .lastCheckIn(LocalDate.now().minusDays(1).atTime(8, 0))
                        .lastCheckOut(LocalDate.now().minusDays(1).atTime(12, 0))
                        .consecutiveOnTimeDays(4)
                        .build())
                .payroll(StaffDashboardDTO.PayrollSnapshot.builder()
                        .estimatedCurrentCycle(BigDecimal.valueOf(400))
                        .bonusTotal(BigDecimal.valueOf(60))
                        .penaltyTotal(BigDecimal.ZERO)
                        .adjustmentNet(BigDecimal.valueOf(60))
                        .lastCyclePaid(BigDecimal.valueOf(380))
                        .build())
                .taskReminders(Collections.emptyList())
                .announcements(Collections.emptyList())
                .build();
    }
}
