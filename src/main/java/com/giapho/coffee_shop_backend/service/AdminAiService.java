package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsRequest;
import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsResponse;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceCheckRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceRecordResponseDTO;
import com.giapho.coffee_shop_backend.integration.gemini.GeminiClient;
import com.giapho.coffee_shop_backend.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiService {

    private final GeminiClient geminiClient;
    private final DashboardAnalyticsService analyticsService;

    public AdminAnalyticsResponse generateInsight(AdminAnalyticsRequest request) {
        DashboardMetricsDTO metrics = analyticsService.collectMetrics(
                request.from(),
                request.to(),
                request.includeTopProducts(),
                request.includeVoucherStats(),
                request.includeCustomerStats()
        );

        String prompt = PromptBuilder.buildPrompt(request, metrics);
        String aiContent = callModel(prompt)
                .orElse("Không thể tạo phân tích tự động vào lúc này. Vui lòng xem dữ liệu thống kê được trả về.");

        return AdminAnalyticsResponse.builder()
                .request(request)
                .metrics(metrics)
                .aiInsightMarkdown(aiContent)
                .build();
    }

    private Optional<String> callModel(String prompt) {
        return geminiClient.generateContent(prompt);
    }

    public static interface AttendanceService {

        AttendanceRecordResponseDTO checkIn(AttendanceCheckRequestDTO request);

        AttendanceRecordResponseDTO checkOut(AttendanceCheckRequestDTO request);

        List<AttendanceRecordResponseDTO> getAttendanceForAssignment(Long assignmentId);

        List<AttendanceRecordResponseDTO> getAttendanceForShift(Long shiftId);
    }
}
