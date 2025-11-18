package com.giapho.coffee_shop_backend.service.dashboard;

import com.giapho.coffee_shop_backend.domain.enums.DashboardRange;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.service.dashboard.helper.CurrentUserResolver;
import com.giapho.coffee_shop_backend.service.dashboard.helper.DashboardDateResolver;
import com.giapho.coffee_shop_backend.service.impl.RoleDashboardServiceImpl;
import com.giapho.coffee_shop_backend.service.dashboard.provider.AdminDashboardProvider;
import com.giapho.coffee_shop_backend.service.dashboard.provider.ManagerDashboardProvider;
import com.giapho.coffee_shop_backend.service.dashboard.provider.StaffDashboardProvider;
import com.giapho.coffee_shop_backend.util.DateRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleDashboardServiceTest {

    @Mock
    private DashboardDateResolver dateResolver;
    @Mock
    private CurrentUserResolver currentUserResolver;
    @Mock
    private AdminDashboardProvider adminDashboardProvider;
    @Mock
    private ManagerDashboardProvider managerDashboardProvider;
    @Mock
    private StaffDashboardProvider staffDashboardProvider;

    @InjectMocks
    private RoleDashboardServiceImpl roleDashboardService;

    private DateRange fixedRange;
    private final LocalDate from = LocalDate.of(2025, 1, 1);
    private final LocalDate to = LocalDate.of(2025, 1, 7);

    @BeforeEach
    void setUp() {
        fixedRange = DateRange.from(null, from, to);
        when(dateResolver.resolve(any(), any(), any())).thenReturn(fixedRange);
    }

    @Test
    void buildAdminDashboard_shouldDelegateToProvider() {
        AdminDashboardDTO expected = AdminDashboardDTO.builder().build();
        when(adminDashboardProvider.build(fixedRange)).thenReturn(expected);

        AdminDashboardDTO result = roleDashboardService.buildAdminDashboard(DashboardRange.MONTH, from, to);

        assertSame(expected, result);
        verify(dateResolver).resolve(DashboardRange.MONTH, from, to);
        verify(adminDashboardProvider).build(fixedRange);
    }

    @Test
    void buildManagerDashboard_shouldDelegateToProvider() {
        ManagerDashboardDTO expected = ManagerDashboardDTO.builder().build();
        when(managerDashboardProvider.build(fixedRange)).thenReturn(expected);

        ManagerDashboardDTO result = roleDashboardService.buildManagerDashboard(DashboardRange.WEEK, from, to);

        assertSame(expected, result);
        verify(dateResolver).resolve(DashboardRange.WEEK, from, to);
        verify(managerDashboardProvider).build(fixedRange);
    }

    @Test
    void buildStaffDashboard_shouldResolveUserBeforeDelegating() {
        Long currentUserId = 88L;
        when(currentUserResolver.resolveOrCurrent(null)).thenReturn(currentUserId);
        StaffDashboardDTO expected = StaffDashboardDTO.builder().build();
        when(staffDashboardProvider.build(currentUserId, fixedRange)).thenReturn(expected);

        StaffDashboardDTO result = roleDashboardService.buildStaffDashboard(null, DashboardRange.LAST_30_DAYS, from, to);

        assertSame(expected, result);
        verify(dateResolver).resolve(DashboardRange.LAST_30_DAYS, from, to);
        verify(currentUserResolver).resolveOrCurrent(null);
        verify(staffDashboardProvider).build(currentUserId, fixedRange);
    }
}
