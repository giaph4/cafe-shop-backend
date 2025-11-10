package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CafeTableStatusUpdateRequest(
        @NotBlank(message = "Status is required") String status
) {
}
