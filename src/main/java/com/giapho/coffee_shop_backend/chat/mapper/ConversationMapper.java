package com.giapho.coffee_shop_backend.chat.mapper;

import com.giapho.coffee_shop_backend.chat.domain.entity.Conversation;
import com.giapho.coffee_shop_backend.chat.domain.entity.ConversationMember;
import com.giapho.coffee_shop_backend.chat.domain.entity.Message;
import com.giapho.coffee_shop_backend.chat.dto.ConversationMemberDTO;
import com.giapho.coffee_shop_backend.chat.dto.ConversationSummaryDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageAttachmentDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import com.giapho.coffee_shop_backend.domain.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ConversationMapper {

    public ConversationSummaryDTO toSummary(Conversation conversation,
                                            Message lastMessage,
                                            int unreadCount,
                                            boolean pinned) {
        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .avatarUrl(conversation.getAvatarUrl())
                .type(conversation.getType().name())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessage(lastMessage != null ? toMessage(lastMessage, false) : null)
                .unreadCount(unreadCount)
                .pinned(pinned)
                .participants(toMembers(conversation.getMembers()))
                .build();
    }

    public List<ConversationMemberDTO> toMembers(Set<ConversationMember> members) {
        return members.stream()
                .map(this::toMember)
                .collect(Collectors.toList());
    }

    public ConversationMemberDTO toMember(ConversationMember member) {
        User user = member.getUser();
        return ConversationMemberDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getRole())
                .pinned(member.isPinned())
                .muted(member.isMuted())
                .lastReadMessageId(member.getLastReadMessageId())
                .build();
    }

    public MessageDTO toMessage(Message message, boolean includeSeen) {
        return MessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatarUrl())
                .content(message.getContent())
                .contentType(message.getContentType())
                .status(message.getStatus())
                .metadata(message.getMetadata())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .attachments(message.getAttachments().stream()
                        .map(this::toAttachment)
                        .collect(Collectors.toList()))
                .seenByUserIds(includeSeen ? message.getSeen().stream()
                        .map(seen -> seen.getUser().getId())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private MessageAttachmentDTO toAttachment(com.giapho.coffee_shop_backend.chat.domain.entity.MessageAttachment attachment) {
        return MessageAttachmentDTO.builder()
                .id(attachment.getId())
                .originalName(attachment.getOriginalName())
                .storedUrl(attachment.getStoredUrl())
                .previewUrl(attachment.getPreviewUrl())
                .mimeType(attachment.getMimeType())
                .size(attachment.getSize())
                .build();
    }
}
