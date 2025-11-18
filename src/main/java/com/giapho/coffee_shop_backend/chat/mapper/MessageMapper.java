package com.giapho.coffee_shop_backend.chat.mapper;

import com.giapho.coffee_shop_backend.chat.domain.entity.Message;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageAttachment;
import com.giapho.coffee_shop_backend.chat.dto.MessageAttachmentDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MessageMapper {

    public MessageDTO toDto(Message message, boolean includeSeen) {
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
                .attachments(toAttachmentDtos(message.getAttachments()))
                .seenByUserIds(includeSeen ? message.getSeen().stream()
                        .map(seen -> seen.getUser().getId())
                        .collect(Collectors.toList()) : null)
                .build();
    }

    public List<MessageDTO> toDtos(List<Message> messages, boolean includeSeen) {
        return messages.stream()
                .map(message -> toDto(message, includeSeen))
                .collect(Collectors.toList());
    }

    private List<MessageAttachmentDTO> toAttachmentDtos(Collection<MessageAttachment> attachments) {
        return attachments.stream()
                .map(this::toAttachmentDto)
                .collect(Collectors.toList());
    }

    private MessageAttachmentDTO toAttachmentDto(MessageAttachment attachment) {
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
