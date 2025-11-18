package com.giapho.coffee_shop_backend.controller.dashboard;

import com.giapho.coffee_shop_backend.controller.AdminDashboardController;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
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
class AdminDashboardControllerTest {

    @Mock
    private RoleDashboardService roleDashboardService;

    @InjectMocks
    private AdminDashboardController controller;

    @Test
    void getAdminDashboard_shouldReturnDashboard() {
        AdminDashboardDTO dashboard = AdminDashboardDTO.builder()
                .revenue(AdminDashboardDTO.RevenueSnapshot.builder()
                        .today(BigDecimal.valueOf(1000))
                        .month(BigDecimal.valueOf(5000))
                        .year(BigDecimal.valueOf(60000))
                        .averageOrderValue(BigDecimal.valueOf(50))
                        .todayProfit(BigDecimal.valueOf(200))
                        .monthProfit(BigDecimal.valueOf(1500))
                        .build())
                .orders(AdminDashboardDTO.OrderSnapshot.builder()
                        .today(20)
                        .month(200)
                        .year(2400)
                        .cancelledToday(1)
                        .cancelledMonth(5)
                        .build())
                .inventory(AdminDashboardDTO.InventorySnapshot.builder()
                        .lowStockItems(3)
                        .totalSuppliers(10)
                        .pendingPurchaseOrders(2)
                        .build())
                .topStaff(Collections.emptyList())
                .topProducts(Collections.emptyList())
                .topCustomers(Collections.emptyList())
                .alerts(Collections.emptyList())
                .build();

        when(roleDashboardService.buildAdminDashboard(null, null, null)).thenReturn(dashboard);

        ResponseEntity<AdminDashboardDTO> response = controller.getAdminDashboard(null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dashboard);
        verify(roleDashboardService).buildAdminDashboard(null, null, null);
    }

    @Test
    void controllerShouldRequireAdminRole() {
        PreAuthorize preAuthorize = controller.getClass().getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }
}
