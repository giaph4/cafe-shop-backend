package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierDTO {

    private Long id;

    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String address;
}