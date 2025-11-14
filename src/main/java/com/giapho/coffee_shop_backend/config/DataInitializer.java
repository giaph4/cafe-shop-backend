package com.giapho.coffee_shop_backend.config;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User; 
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository; 
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.stereotype.Component;

import java.util.Set; 

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository; 
    private final PasswordEncoder passwordEncoder; 

    @Value("${app.seed.admin.username:admin}")
    private String seedAdminUsername;

    @Value("${app.seed.admin.password:ChangeMe123!}")
    private String seedAdminPassword;

    @Value("${app.seed.admin.email:admin@example.com}")
    private String seedAdminEmail;

    @Value("${app.seed.admin.full-name:Default Admin}")
    private String seedAdminFullName;

    @Value("${app.seed.admin.phone:0000000000}")
    private String seedAdminPhone;

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
        log.info("Checking for seeded admin user '{}'", seedAdminUsername);
        if (userRepository.findByUsername(seedAdminUsername).isEmpty()) {
            // Lấy ROLE_ADMIN vừa tạo
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Cannot find ROLE_ADMIN"));

            // Mã hóa mật khẩu
            String encodedPassword = passwordEncoder.encode(seedAdminPassword);

            // Tạo user mới
            User adminUser = User.builder()
                    .username(seedAdminUsername)
                    .password(encodedPassword)
                    .fullName(seedAdminFullName)
                    .email(seedAdminEmail)
                    .phone(seedAdminPhone)
                    .status("ACTIVE") // Rất quan trọng, phải là "ACTIVE" để đăng nhập
                    .roles(Set.of(adminRole)) // Gán quyền admin
                    .build();

            // Lưu vào CSDL
            userRepository.save(adminUser);
            log.info("Created default admin user '{}'", seedAdminUsername);
        } else {
            log.info("Seed admin user '{}' already exists.", seedAdminUsername);
        }
    }
}