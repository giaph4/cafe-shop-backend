package com.giapho.coffee_shop_backend.chat.service;

import com.giapho.coffee_shop_backend.chat.dto.ConversationMemberDTO;
import com.giapho.coffee_shop_backend.chat.dto.ConversationSummaryDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.util.List;

public interface ConversationService {

    Page<ConversationSummaryDTO> listUserConversations(Pageable pageable);

    ConversationSummaryDTO getConversation(Long conversationId);

    ConversationSummaryDTO createDirectConversation(Long targetUserId);

    ConversationSummaryDTO createGroupConversation(String title, List<Long> memberIds);

    List<ConversationMemberDTO> listMembers(Long conversationId);

    void addMembers(Long conversationId, List<Long> memberIds);

    void removeMember(Long conversationId, Long memberId);

    void updateMemberRole(Long conversationId, Long memberId, String role);

    void pinConversation(Long conversationId, boolean pinned);

    Page<MessageDTO> listMessages(Long conversationId, @Nullable Long beforeMessageId, Pageable pageable);

    MessageDTO sendTextMessage(Long conversationId, String content);
}
