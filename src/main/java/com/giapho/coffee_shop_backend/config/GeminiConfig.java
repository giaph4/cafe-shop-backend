package com.giapho.coffee_shop_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cho Gemini AI
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiConfig(
    String apiKey,
    String model,
    String apiVersion,
    Double temperature,
    String baseUrl
) {
    public GeminiConfig {
        if (apiVersion == null) {
            apiVersion = "v1";
        }
        if (temperature == null) {
            temperature = 0.3;
        }
        if (baseUrl == null) {
            baseUrl = "https://generativelanguage.googleapis.com";
        }
    }
}
