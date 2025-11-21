package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsRequest;
import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsResponse;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import com.giapho.coffee_shop_backend.integration.gemini.GeminiClient;
import com.giapho.coffee_shop_backend.service.AdminAiService;
import com.giapho.coffee_shop_backend.service.DashboardAnalyticsService;
import com.giapho.coffee_shop_backend.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAiServiceImpl implements AdminAiService {

    private static final String FALLBACK_MESSAGE = "Không thể tạo phân tích tự động vào lúc này. Vui lòng xem dữ liệu thống kê được trả về.";

    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final GeminiClient geminiClient;

    @Override
    public AdminAnalyticsResponse generateInsight(AdminAnalyticsRequest request) {
        DashboardMetricsDTO metrics = dashboardAnalyticsService.collectMetrics(
                request.from(),
                request.to(),
                request.includeTopProducts(),
                request.includeVoucherStats(),
                request.includeCustomerStats()
        );

        String prompt = PromptBuilder.buildPrompt(request, metrics);
        String aiContent = geminiClient.generateContent(prompt)
                .filter(StringUtils::hasText)
                .orElseGet(() -> {
                    log.warn("Gemini không trả về nội dung hợp lệ cho yêu cầu quản trị viên");
                    return FALLBACK_MESSAGE;
                });

        return AdminAnalyticsResponse.builder()
                .request(request)
                .metrics(metrics)
                .aiInsightMarkdown(aiContent)
                .build();
    }
}
