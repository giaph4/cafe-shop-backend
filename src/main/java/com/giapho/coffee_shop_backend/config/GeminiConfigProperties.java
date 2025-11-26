package com.giapho.coffee_shop_backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiConfig.class)
public class GeminiConfigProperties {
    // This class enables @ConfigurationProperties for GeminiConfig
}
