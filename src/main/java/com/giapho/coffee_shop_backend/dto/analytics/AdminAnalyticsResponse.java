package com.giapho.coffee_shop_backend.dto.analytics;

import lombok.Builder;

@Builder
public record AdminAnalyticsResponse(
        AdminAnalyticsRequest request,
        DashboardMetricsDTO metrics,
        String aiInsightMarkdown
) {
}
