package com.giapho.coffee_shop_backend.chat.dto;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class MessageAttachmentDTO {
    Long id;
    String originalName;
    String storedUrl;
    String previewUrl;
    String mimeType;
    Long size;
}
