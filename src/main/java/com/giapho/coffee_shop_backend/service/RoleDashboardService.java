package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.enums.DashboardRange;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import java.time.LocalDate;

public interface RoleDashboardService {

    AdminDashboardDTO buildAdminDashboard(DashboardRange range, LocalDate from, LocalDate to);

    default AdminDashboardDTO buildAdminDashboard() {
        return buildAdminDashboard(null, null, null);
    }

    ManagerDashboardDTO buildManagerDashboard(DashboardRange range, LocalDate from, LocalDate to);

    default ManagerDashboardDTO buildManagerDashboard() {
        return buildManagerDashboard(null, null, null);
    }

    StaffDashboardDTO buildStaffDashboard(Long userId, DashboardRange range, LocalDate from, LocalDate to);

    default StaffDashboardDTO buildStaffDashboard() {
        return buildStaffDashboard(null, null, null, null);
    }

    default StaffDashboardDTO buildStaffDashboard(Long userId) {
        return buildStaffDashboard(userId, null, null, null);
    }

    void evictAllCaches();
}
