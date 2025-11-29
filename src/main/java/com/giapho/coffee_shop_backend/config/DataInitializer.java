package com.giapho.coffee_shop_backend.config;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Bắt đầu khởi tạo dữ liệu mẫu...");

        // 1. Tạo các role nếu chưa tồn tại (đảm bảo hệ thống có đầy đủ roles)
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    log.info("Tạo mới ROLE_ADMIN");
                    return roleRepository.save(Role.builder().name("ROLE_ADMIN").build());
                });

        // Tạo ROLE_MANAGER và ROLE_STAFF để đảm bảo hệ thống có đầy đủ roles
        roleRepository.findByName("ROLE_MANAGER")
                .orElseGet(() -> {
                    log.info("Tạo mới ROLE_MANAGER");
                    return roleRepository.save(Role.builder().name("ROLE_MANAGER").build());
                });

        roleRepository.findByName("ROLE_STAFF")
                .orElseGet(() -> {
                    log.info("Tạo mới ROLE_STAFF");
                    return roleRepository.save(Role.builder().name("ROLE_STAFF").build());
                });

        // 2. Tạo tài khoản mẫu huynhpho với quyền ADMIN nếu chưa tồn tại
        userRepository.findByUsername("huynhpho")
                .ifPresentOrElse(
                        user -> log.info("Tài khoản huynhpho đã tồn tại"),
                        () -> {
                            User sampleUser = User.builder()
                                    .username("huynhpho")
                                    .password(passwordEncoder.encode("123456"))
                                    .email("huynhpho@example.com")
                                    .fullName("Huynh Pho")
                                    .phone("0123456789")
                                    .status("ACTIVE")
                                    .roles(Set.of(adminRole))
                                    .build();
                            userRepository.save(sampleUser);
                            log.info("\n========================================\n" +
                                    "ĐÃ TẠO TÀI KHOẢN MẪU\n" +
                                    "Username: huynhpho\n" +
                                    "Password: 123456\n" +
                                    "Role: ROLE_ADMIN\n" +
                                    "========================================");
                        }
                );

        log.info("Hoàn tất khởi tạo dữ liệu mẫu");
    }
}