package com.giapho.coffee_shop_backend.chat.dto;

import com.giapho.coffee_shop_backend.chat.domain.enums.ConversationMemberRole;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ConversationMemberDTO {
    Long userId;
    String username;
    String fullName;
    String avatarUrl;
    ConversationMemberRole role;
    boolean pinned;
    boolean muted;
    Long lastReadMessageId;
}
