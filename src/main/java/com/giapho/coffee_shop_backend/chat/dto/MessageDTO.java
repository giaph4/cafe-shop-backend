package com.giapho.coffee_shop_backend.chat.dto;

import com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus;
import com.giapho.coffee_shop_backend.chat.domain.enums.MessageType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Value
public class MessageDTO {
    Long id;
    Long conversationId;
    Long senderId;
    String senderName;
    String senderAvatar;
    String content;
    MessageType contentType;
    MessageStatus status;
    String metadata;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<MessageAttachmentDTO> attachments;
    List<Long> seenByUserIds;
}
