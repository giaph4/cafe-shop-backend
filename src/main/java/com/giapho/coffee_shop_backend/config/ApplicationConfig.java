package com.giapho.coffee_shop_backend.config;

import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    /**
      Định nghĩa một Bean (đối tượng) UserDetailsService.
     *Spring Security sẽ dùng Bean này để lấy thông tin User từ database.
     */

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * Định nghĩa Bean PasswordEncoder.
     * Dùng BCrypt, là thuật toán mã hoá một chiều mạnh và tiêu chuẩn hiện nay.
     */

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Định nghĩa Bean AuthenticationProvider.
     * Đây là "cỗ máy" xác thực. Nó sẽ làm 2 việc:
     * 1. Lấy thông tin user (dùng userDetailsService() ở trên).
     * 2. So sánh mật khẩu (dùng passwordEncoder() ở trên).
     */

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // Cung cấp dịch vụ tìm user
        authProvider.setPasswordEncoder(passwordEncoder()); // Cung cấp trình mã hoá mật khẩu
        return authProvider;
    }

    /**
            * Định nghĩa Bean AuthenticationManager.
            * Đây là Bean mà chúng ta sẽ dùng ở Controller để kích hoạt quá trình đăng nhập.
     */

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
