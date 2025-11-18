package com.giapho.coffee_shop_backend.chat.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Value
public class ConversationSummaryDTO {
    Long id;
    String title;
    String avatarUrl;
    String type;
    LocalDateTime updatedAt;
    MessageDTO lastMessage;
    int unreadCount;
    boolean pinned;
    List<ConversationMemberDTO> participants;
}
