package com.giapho.coffee_shop_backend.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupConversationRequest(
        @NotBlank String title,
        @Size(max = 100) List<Long> memberIds
) {
}
