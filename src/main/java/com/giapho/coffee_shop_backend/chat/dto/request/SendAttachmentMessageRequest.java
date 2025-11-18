package com.giapho.coffee_shop_backend.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record SendAttachmentMessageRequest(
        @Size(max = 500) String messageText,
        @NotEmpty List<MultipartFile> files
) {
}
