package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.service.RoleDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class StaffDashboardController {

    private final RoleDashboardService roleDashboardService;

    @GetMapping
    public ResponseEntity<StaffDashboardDTO> getCurrentStaffDashboard(
            @RequestParam(name = "userId", required = false) Long userId
    ) {
        return ResponseEntity.ok(roleDashboardService.buildStaffDashboard(userId));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<StaffDashboardDTO> getStaffDashboardForManager(@PathVariable Long userId) {
        return ResponseEntity.ok(roleDashboardService.buildStaffDashboard(userId));
    }
}
