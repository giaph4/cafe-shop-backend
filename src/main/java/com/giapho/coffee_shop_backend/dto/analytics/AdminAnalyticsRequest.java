package com.giapho.coffee_shop_backend.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdminAnalyticsRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotBlank String question,
        boolean includeTopProducts,
        boolean includeVoucherStats,
        boolean includeCustomerStats
) {
}
