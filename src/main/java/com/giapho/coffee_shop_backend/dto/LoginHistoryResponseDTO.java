package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String status;
    private Boolean success;
    private LocalDateTime loginAt;
    private String ipAddress;
    private String userAgent;
    private String message;
}
