package com.giapho.coffee_shop_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.security.filter.disable=true",
        "application.jwt.secretKey=TEST_SECRET_KEY_32_CHAR_MINIMUM_TEST_SECRET_KEY",
        "application.jwt.expirationMs=3600000",
        "app.cors.allowed-origins=http://localhost"
})
class CoffeeShopBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
