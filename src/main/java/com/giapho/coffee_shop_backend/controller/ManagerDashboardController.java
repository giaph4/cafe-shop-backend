package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.service.RoleDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerDashboardController {

    private final RoleDashboardService roleDashboardService;

    @GetMapping
    public ResponseEntity<ManagerDashboardDTO> getManagerDashboard() {
        return ResponseEntity.ok(roleDashboardService.buildManagerDashboard());
    }
}
