package com.giapho.coffee_shop_backend.controller.dashboard;

import com.giapho.coffee_shop_backend.controller.ManagerDashboardController;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardControllerTest {

    @Mock
    private RoleDashboardService roleDashboardService;

    @InjectMocks
    private ManagerDashboardController controller;

    @Test
    void getManagerDashboard_shouldReturnDashboard() {
        ManagerDashboardDTO dashboard = ManagerDashboardDTO.builder()
                .shiftOverview(ManagerDashboardDTO.ShiftOverview.builder()
                        .scheduledToday(5)
                        .inProgress(2)
                        .completed(8)
                        .cancelled(1)
                        .upcomingShifts(Collections.emptyList())
                        .build())
                .teamPerformance(ManagerDashboardDTO.TeamPerformance.builder()
                        .totalRevenue(BigDecimal.valueOf(4200))
                        .totalOrders(75)
                        .averageOrderValue(BigDecimal.valueOf(56))
                        .topStaff(Collections.emptyList())
                        .build())
                .inventory(ManagerDashboardDTO.InventoryFocus.builder()
                        .lowStockItems(4)
                        .criticalStockItems(1)
                        .alerts(Collections.emptyList())
                        .build())
                .payroll(ManagerDashboardDTO.PayrollOverview.builder()
                        .estimatedPayroll(BigDecimal.valueOf(1800))
                        .bonusTotal(BigDecimal.valueOf(250))
                        .penaltyTotal(BigDecimal.valueOf(30))
                        .adjustmentNet(BigDecimal.valueOf(220))
                        .staffCount(12)
                        .build())
                .pendingApprovals(Collections.emptyList())
                .attendanceAlerts(Collections.emptyList())
                .serviceIssues(Collections.emptyList())
                .build();

        when(roleDashboardService.buildManagerDashboard()).thenReturn(dashboard);

        ResponseEntity<ManagerDashboardDTO> response = controller.getManagerDashboard();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dashboard);
        verify(roleDashboardService).buildManagerDashboard();
    }

    @Test
    void controllerShouldAllowManagerAndAdminRoles() {
        PreAuthorize preAuthorize = controller.getClass().getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("'MANAGER'");
    }
}
