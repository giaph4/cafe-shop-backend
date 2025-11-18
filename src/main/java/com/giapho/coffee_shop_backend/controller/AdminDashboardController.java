package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.domain.enums.DashboardRange;
import com.giapho.coffee_shop_backend.service.RoleDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final RoleDashboardService roleDashboardService;

    @GetMapping
    public ResponseEntity<AdminDashboardDTO> getAdminDashboard(
            @RequestParam(name = "range", required = false) DashboardRange range,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(roleDashboardService.buildAdminDashboard(range, from, to));
    }
}
