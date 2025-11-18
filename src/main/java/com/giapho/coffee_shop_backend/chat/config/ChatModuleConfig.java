package com.giapho.coffee_shop_backend.chat.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình gốc cho module chat, chịu trách nhiệm bật quét cấu hình và
 * đảm bảo các bean tiện ích được khởi tạo trước khi sử dụng.
 */
@Configuration
@ConfigurationPropertiesScan(basePackages = "com.giapho.coffee_shop_backend.chat")
public class ChatModuleConfig {
}
