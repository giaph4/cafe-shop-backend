package com.giapho.coffee_shop_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CoffeeShopBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeShopBackendApplication.class, args);
    }

}
