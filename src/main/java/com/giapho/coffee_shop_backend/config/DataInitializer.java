package com.giapho.coffee_shop_backend.config;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j // Lombok annotation để tự động tạo logger
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking for default roles...");

        // Tạo ROLE_STAFF nếu chưa tồn tại
        if (roleRepository.findByName("ROLE_STAFF").isEmpty()) {
            Role staffRole = Role.builder().name("ROLE_STAFF").build();
            roleRepository.save(staffRole);
            log.info("Created ROLE_STAFF");
        }

        // Tạo ROLE_MANAGER nếu chưa tồn tại
        if (roleRepository.findByName("ROLE_MANAGER").isEmpty()) {
            Role managerRole = Role.builder().name("ROLE_MANAGER").build();
            roleRepository.save(managerRole);
            log.info("Created ROLE_MANAGER");
        }

        // Tạo ROLE_ADMIN nếu chưa tồn tại
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            Role adminRole = Role.builder().name("ROLE_ADMIN").build();
            roleRepository.save(adminRole);
            log.info("Created ROLE_ADMIN");
        }

        log.info("Default roles check complete.");
    }
}