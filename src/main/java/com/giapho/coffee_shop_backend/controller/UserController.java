package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.ChangePasswordRequestDTO;
import com.giapho.coffee_shop_backend.dto.UserResponseDTO;
import com.giapho.coffee_shop_backend.dto.UserUpdateRequestDTO;
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

@RestController
@RequestMapping("/api/v1/users") // Endpoint chung cho quản lý user
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * API Lấy danh sách tất cả người dùng (phân trang)
     * Chỉ ADMIN mới có quyền xem danh sách user.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @PageableDefault(size = 15, page = 0, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<UserResponseDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * API Lấy chi tiết một người dùng theo ID
     * Chỉ ADMIN mới có quyền xem chi tiết user khác.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * API Cập nhật thông tin và quyền của người dùng
     * Chỉ ADMIN mới có quyền cập nhật user khác.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO updateDTO
    ) {
        UserResponseDTO updatedUser = userService.updateUser(id, updateDTO);
        return ResponseEntity.ok(updatedUser);
    }

    // (Lưu ý: Thường không nên có API XÓA user vĩnh viễn,
    // thay vào đó nên dùng API cập nhật status='INACTIVE' hoặc 'TERMINATED')

    /**
     * API cho phép người dùng đang đăng nhập tự đổi mật khẩu
     * Yêu cầu người dùng phải được xác thực (có token hợp lệ).
     * Dùng POST hoặc PATCH đều được.
     */
    @PostMapping("/change-password") // Hoặc @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()") // Chỉ cần đăng nhập là được
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        userService.changePassword(request);
        return ResponseEntity.ok("Password changed successfully");
    }
}