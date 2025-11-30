package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.*;
import com.giapho.coffee_shop_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<UserResponseDTO> registerUser(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        UserResponseDTO createdUser = userService.registerUser(registerRequest);
        return ResponseEntity.ok(createdUser);
    }


    // lay tat ca roles
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        List<RoleDTO> roles = userService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @PageableDefault(size = 15, page = 0, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<UserResponseDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id || hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id || hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO updateDTO
    ) {
        UserResponseDTO updatedUser = userService.updateUser(id, updateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        userService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        String tempPassword = userService.resetPassword(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mật khẩu đã được đặt lại. Vui lòng kiểm tra email của bạn.");
        response.put("temporaryPassword", tempPassword); // Only include in development
        return ResponseEntity.ok(response);
    }

}