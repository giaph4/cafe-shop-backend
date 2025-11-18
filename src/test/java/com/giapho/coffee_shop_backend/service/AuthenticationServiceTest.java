package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.RoleRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.AuthenticationResponse;
import com.giapho.coffee_shop_backend.dto.LoginRequest;
import com.giapho.coffee_shop_backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LoginHistoryService loginHistoryService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_ShouldRecordSuccessfulAttempt() {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("password")
                .build();

        User user = User.builder()
                .id(1L)
                .username("admin")
                .status("ACTIVE")
                .build();

        when(userRepository.findWithRolesByUsername("admin")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("token");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        AuthenticationResponse response = authenticationService.login(request, httpServletRequest);

        assertThat(response.getToken()).isEqualTo("token");
        verify(loginHistoryService).recordSuccessfulLogin(eq(user), eq("127.0.0.1"), eq("JUnit"));
        verify(loginHistoryService, never()).recordFailedLogin(any(), any(), any(), any());
    }

    @Test
    void login_ShouldRecordFailedAttempt_WhenAuthenticationFails() {
        LoginRequest request = LoginRequest.builder()
                .username("admin")
                .password("wrong")
                .build();

        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.2");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request, httpServletRequest));

        verify(loginHistoryService).recordFailedLogin(eq("admin"), eq("192.168.1.2"), eq("JUnit"), eq("Invalid username or password"));
        verify(loginHistoryService, never()).recordSuccessfulLogin(any(), any(), any());
    }

    @Test
    void login_ShouldRecordFailedAttempt_WhenUserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .username("missing")
                .password("password")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findWithRolesByUsername("missing")).thenReturn(java.util.Optional.empty());
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("JUnit");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpServletRequest.getRemoteAddr()).thenReturn("10.0.0.1");

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request, httpServletRequest));

        verify(loginHistoryService).recordFailedLogin(eq("missing"), eq("10.0.0.1"), eq("JUnit"), eq("Invalid username or password"));
        verify(loginHistoryService, never()).recordSuccessfulLogin(any(), any(), any());
    }
}
