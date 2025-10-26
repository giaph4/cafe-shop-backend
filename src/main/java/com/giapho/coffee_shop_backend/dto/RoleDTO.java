package com.giapho.coffee_shop_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    private Long id;
    private String name; // Ví dụ: "ROLE_STAFF", "ROLE_MANAGER"
}