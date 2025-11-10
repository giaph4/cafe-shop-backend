package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.AuthenticationResponse;
import com.giapho.coffee_shop_backend.dto.LoginRequest;
import com.giapho.coffee_shop_backend.dto.RegisterRequest;
import com.giapho.coffee_shop_backend.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginHistoryService loginHistoryService;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Error: Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        Role staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new EntityNotFoundException("Error: Role 'STAFF' not found"));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status("ACTIVE")
                .roles(Set.of(staffRole))
                .build();

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .build();
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        String username = request.getUsername();
        String ipAddress = extractClientIp(httpServletRequest);
        String userAgent = httpServletRequest != null ? httpServletRequest.getHeader("User-Agent") : null;
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            loginHistoryService.recordFailedLogin(
                    username,
                    ipAddress,
                    userAgent,
                    "Invalid username or password"
            );
            throw new EntityNotFoundException("Invalid username or password");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    loginHistoryService.recordFailedLogin(
                            username,
                            ipAddress,
                            userAgent,
                            "User not found"
                    );
                    return new EntityNotFoundException("User not found");
                });

        String jwtToken = jwtService.generateToken(user);

        loginHistoryService.recordSuccessfulLogin(user, ipAddress, userAgent);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .username(user.getUsername())
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
