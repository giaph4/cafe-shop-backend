package com.giapho.coffee_shop_backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String address;
    private String status;
    private String statusMessage;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<RoleDTO> roles;
}