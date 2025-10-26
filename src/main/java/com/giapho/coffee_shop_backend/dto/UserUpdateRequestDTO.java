package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Set;

@Data
public class UserUpdateRequestDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?84|0)\\d{9}$", message = "Invalid Vietnamese phone number format")
    private String phone;

    @Email(message = "Invalid email format")
    private String email; // Cho phép null

    @NotBlank(message = "Status is required (e.g., ACTIVE, INACTIVE)")
    private String status; // Trạng thái tài khoản

    @NotEmpty(message = "User must have at least one role")
    private Set<Long> roleIds; // Danh sách ID của các quyền muốn gán
}