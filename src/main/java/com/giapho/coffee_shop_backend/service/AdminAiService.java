package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsRequest;
import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsResponse;

public interface AdminAiService {

    AdminAnalyticsResponse generateInsight(AdminAnalyticsRequest request);
}
