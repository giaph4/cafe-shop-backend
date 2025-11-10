package com.giapho.coffee_shop_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Coffee Shop Backend API",
                version = "1.0.0",
                description = "Tài liệu REST API cho hệ thống quản lý quán cà phê.",
                contact = @Contact(name = "Windsurf Team", email = "support@example.com"),
                license = @License(name = "Proprietary", url = "https://example.com/license")
        )
)
@Configuration
public class OpenApiConfig {
}
