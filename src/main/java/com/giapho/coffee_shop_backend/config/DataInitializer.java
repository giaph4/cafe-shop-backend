package com.giapho.coffee_shop_backend.config;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User; // Thêm import
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository; // Thêm import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder; // Thêm import
import org.springframework.stereotype.Component;

import java.util.Set; // Thêm import

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository; // Thêm UserRepository
    private final PasswordEncoder passwordEncoder; // Thêm PasswordEncoder

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

        // === TẠO TÀI KHOẢN ADMIN MẪU ===
        log.info("Checking for default admin user...");
        if (userRepository.findByUsername("giapho").isEmpty()) {
            // Lấy ROLE_ADMIN vừa tạo
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Cannot find ROLE_ADMIN"));

            // Mã hóa mật khẩu
            String encodedPassword = passwordEncoder.encode("123456");

            // Tạo user mới
            User adminUser = User.builder()
                    .username("giapho")
                    .password(encodedPassword)
                    .fullName("Admin Tối Cao")
                    .email("giapho@shop.com")
                    .phone("0123456788")
                    .status("ACTIVE") // Rất quan trọng, phải là "ACTIVE" để đăng nhập
                    .roles(Set.of(adminRole)) // Gán quyền admin
                    .build();

            // Lưu vào CSDL
            userRepository.save(adminUser);
            log.info("Created default admin user (admin/123456)");
        } else {
            log.info("Admin user already exists.");
        }
    }
}