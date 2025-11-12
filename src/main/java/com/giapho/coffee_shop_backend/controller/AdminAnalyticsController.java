package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsRequest;
import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsResponse;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.service.AdminAiService;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAiService adminAiService;
    private final DashboardAnalyticsService dashboardAnalyticsService;

    @PostMapping("/insight")
    public ResponseEntity<AdminAnalyticsResponse> generateInsight(@Valid @RequestBody AdminAnalyticsRequest request) {
        return ResponseEntity.ok(adminAiService.generateInsight(request));
    }

    @GetMapping("/metrics")
    public ResponseEntity<DashboardMetricsDTO> getMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean includeTopProducts,
            @RequestParam(defaultValue = "true") boolean includeVoucherStats,
            @RequestParam(defaultValue = "true") boolean includeCustomerStats
    ) {
        DashboardMetricsDTO metrics = dashboardAnalyticsService.collectMetrics(
                from,
                to,
                includeTopProducts,
                includeVoucherStats,
                includeCustomerStats
        );
        return ResponseEntity.ok(metrics);
    }
}
