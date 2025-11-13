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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Set<Role> roles = new HashSet<>();
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            Role staffRole = roleRepository.findByName("ROLE_STAFF")
                    .orElseThrow(() -> new EntityNotFoundException("Error: Role 'STAFF' not found"));
            roles.add(staffRole);
        } else {
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new EntityNotFoundException("Role not found with id: " + roleId));
                roles.add(role);
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status("ACTIVE")
                .roles(roles)
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
        String userAgent = extractUserAgent(httpServletRequest);
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
        String headerValue = findFirstHeaderValue(request, List.of(
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "X-Forwarded",
                "Forwarded-For",
                "Forwarded",
                "X-Cluster-Client-IP",
                "Client-IP",
                "Fastly-Client-IP",
                "True-Client-IP"
        ));

        if (StringUtils.hasText(headerValue)) {
            return headerValue.split(",")[0].trim();
        }

        String remoteAddr = request.getRemoteAddr();
        if (!StringUtils.hasText(remoteAddr)) {
            return null;
        }
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        return remoteAddr;
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userAgent = findFirstHeaderValue(request, List.of(
                "User-Agent",
                "X-Original-User-Agent",
                "X-Device-User-Agent"
        ));

        if (StringUtils.hasText(userAgent)) {
            return userAgent;
        }

        String platform = sanitizeSecChValue(request.getHeader("sec-ch-ua-platform"));
        String brand = parseSecChUaBrand(request.getHeader("sec-ch-ua"));
        String mobileHeader = request.getHeader("sec-ch-ua-mobile");
        boolean isMobile = "?1".equals(mobileHeader) || "1".equals(mobileHeader);

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(brand)) {
            builder.append(brand);
        }
        if (StringUtils.hasText(platform)) {
            if (builder.length() > 0) {
                builder.append(" on ");
            }
            builder.append(platform);
        }
        if (isMobile) {
            builder.append(" (Mobile)");
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    private String findFirstHeaderValue(HttpServletRequest request, List<String> headers) {
        for (String header : headers) {
            String headerValue = request.getHeader(header);
            if (StringUtils.hasText(headerValue)) {
                return headerValue;
            }
        }
        return null;
    }

    private String sanitizeSecChValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String parseSecChUaBrand(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return null;
        }
        Pattern pattern = Pattern.compile("\"([^\"]+)\";v=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(headerValue);
        while (matcher.find()) {
            String brand = matcher.group(1);
            if ("Not_A Brand".equalsIgnoreCase(brand)) {
                continue;
            }
            String version = matcher.group(2);
            String cleanedBrand = sanitizeSecChValue(brand);
            String cleanedVersion = sanitizeSecChValue(version);
            if (StringUtils.hasText(cleanedVersion)) {
                return cleanedBrand + " " + cleanedVersion;
            }
            return cleanedBrand;
        }
        return null;
    }
}
