package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerDTO {

    private Long id;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(\\+?84|0)\\d{9}$", message = "Invalid Vietnamese phone number format")
    private String phone;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private int loyaltyPoints;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}