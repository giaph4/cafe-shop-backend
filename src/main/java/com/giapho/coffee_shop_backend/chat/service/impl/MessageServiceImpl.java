package com.giapho.coffee_shop_backend.chat.service.impl;

import com.giapho.coffee_shop_backend.chat.domain.entity.Conversation;
import com.giapho.coffee_shop_backend.chat.domain.entity.ConversationMember;
import com.giapho.coffee_shop_backend.chat.domain.entity.Message;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageAttachment;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageDeletion;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageDeletionId;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageSeen;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageSeenId;
import com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus;
import com.giapho.coffee_shop_backend.chat.domain.enums.MessageType;
import com.giapho.coffee_shop_backend.chat.domain.repository.ConversationMemberRepository;
import com.giapho.coffee_shop_backend.chat.domain.repository.ConversationRepository;
import com.giapho.coffee_shop_backend.chat.domain.repository.MessageDeletionRepository;
import com.giapho.coffee_shop_backend.chat.domain.repository.MessageRepository;
import com.giapho.coffee_shop_backend.chat.domain.repository.MessageSeenRepository;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import com.giapho.coffee_shop_backend.chat.exception.ChatAccessDeniedException;
import com.giapho.coffee_shop_backend.chat.exception.ConversationNotFoundException;
import com.giapho.coffee_shop_backend.chat.exception.MessageAccessDeniedException;
import com.giapho.coffee_shop_backend.chat.exception.MessageNotFoundException;
import com.giapho.coffee_shop_backend.chat.exception.MessageValidationException;
import com.giapho.coffee_shop_backend.chat.mapper.MessageMapper;
import com.giapho.coffee_shop_backend.chat.service.MessageService;
import com.giapho.coffee_shop_backend.chat.util.ChatUserResolver;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageDeletionRepository messageDeletionRepository;
    private final MessageSeenRepository messageSeenRepository;
    private final MessageMapper messageMapper;
    private final ChatUserResolver chatUserResolver;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public MessageDTO sendText(Long conversationId, String content) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());

        String normalizedContent = normalizeContent(content);
        if (!StringUtils.hasText(normalizedContent)) {
            throw new MessageValidationException("Nội dung tin nhắn không được để trống");
        }

        Message message = buildBaseMessage(conversation, currentUser, normalizedContent, MessageType.TEXT, null);
        Message saved = messageRepository.save(message);
        touchConversation(conversation, saved);
        markSenderSeen(conversation, currentUser, saved);
        return messageMapper.toDto(saved, true);
    }

    @Override
    @Transactional
    public MessageDTO sendEmoji(Long conversationId, String emojiCode) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());

        String normalized = normalizeContent(emojiCode);
        if (!StringUtils.hasText(normalized)) {
            throw new MessageValidationException("Emoji không hợp lệ");
        }

        Message message = buildBaseMessage(conversation, currentUser, normalized, MessageType.EMOJI, null);
        Message saved = messageRepository.save(message);
        touchConversation(conversation, saved);
        markSenderSeen(conversation, currentUser, saved);
        return messageMapper.toDto(saved, true);
    }

    @Override
    @Transactional
    public MessageDTO sendAttachments(Long conversationId, String messageText, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new MessageValidationException("Tin nhắn đính kèm bắt buộc phải có tệp tải lên");
        }

        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());

        String normalizedText = normalizeContent(messageText);
        if (!StringUtils.hasText(normalizedText)) {
            normalizedText = null;
        }
        MessageType messageType = resolveTypeFromFiles(files);

        Message message = buildBaseMessage(conversation, currentUser, normalizedText, messageType, null);
        attachFiles(message, files);

        Message saved = messageRepository.save(message);
        touchConversation(conversation, saved);
        markSenderSeen(conversation, currentUser, saved);
        return messageMapper.toDto(saved, true);
    }

    @Override
    @Transactional
    public MessageDTO recall(Long messageId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Message message = requireMessage(messageId);
        ensureSender(currentUser, message);

        if (message.getStatus() == MessageStatus.RECALLED) {
            return messageMapper.toDto(message, true);
        }

        deleteAttachmentsFromStorage(message.getAttachments());
        message.getAttachments().clear();

        message.setStatus(MessageStatus.RECALLED);
        message.setContent(null);
        message.setMetadata(null);
        message.setUpdatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        return messageMapper.toDto(saved, true);
    }

    @Override
    @Transactional
    public void deleteForCurrentUser(Long messageId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Message message = requireMessage(messageId);
        requireMemberConversation(message.getConversation().getId(), currentUser.getId());

        MessageDeletionId id = new MessageDeletionId(messageId, currentUser.getId());
        if (messageDeletionRepository.existsById(id)) {
            return;
        }

        MessageDeletion deletion = MessageDeletion.builder()
                .id(id)
                .message(message)
                .user(currentUser)
                .build();
        messageDeletionRepository.save(deletion);
    }

    @Override
    @Transactional
    public Long markSeen(Long conversationId, Long messageId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());
        Message message = requireMessage(messageId);

        if (!message.getConversation().getId().equals(conversation.getId())) {
            throw new ChatAccessDeniedException();
        }

        ConversationMember member = conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, currentUser.getId())
                .orElseThrow(ChatAccessDeniedException::new);
        if (member.getLastReadMessageId() == null || messageId > member.getLastReadMessageId()) {
            member.setLastReadMessageId(messageId);
            member.setUpdatedAt(LocalDateTime.now());
            conversationMemberRepository.save(member);
        }

        MessageSeenId id = new MessageSeenId(messageId, currentUser.getId());
        if (messageSeenRepository.existsById(id)) {
            return currentUser.getId();
        }

        MessageSeen seen = MessageSeen.builder()
                .id(id)
                .message(message)
                .user(currentUser)
                .build();
        messageSeenRepository.save(seen);
        return currentUser.getId();
    }

    private Message buildBaseMessage(Conversation conversation,
                                     User sender,
                                     String content,
                                     MessageType type,
                                     String metadata) {
        return Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .contentType(type)
                .status(MessageStatus.SENT)
                .metadata(StringUtils.hasText(metadata) ? metadata : null)
                .build();
    }

    private void attachFiles(Message message, List<MultipartFile> files) {
        Set<MessageAttachment> attachments = files.stream()
                .map(file -> mapToAttachment(message, file))
                .collect(Collectors.toCollection(HashSet::new));
        message.getAttachments().addAll(attachments);
    }

    private MessageAttachment mapToAttachment(Message message, MultipartFile file) {
        String storedFileName = fileStorageService.storeFile(file);
        String fileUrl = fileStorageService.getFileUrl(storedFileName);

        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");

        return MessageAttachment.builder()
                .message(message)
                .originalName(file.getOriginalFilename())
                .storedUrl(fileUrl)
                .previewUrl(isImageContentType(contentType) ? fileUrl : null)
                .mimeType(contentType)
                .size(file.getSize())
                .build();
    }

    private void deleteAttachmentsFromStorage(Collection<MessageAttachment> attachments) {
        attachments.stream()
                .map(MessageAttachment::getStoredUrl)
                .filter(StringUtils::hasText)
                .map(fileStorageService::extractFileNameFromUrl)
                .filter(StringUtils::hasText)
                .forEach(fileStorageService::deleteFile);
    }

    private Conversation requireMemberConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        boolean member = conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId);
        if (!member) {
            throw new ChatAccessDeniedException();
        }
        return conversation;
    }

    private Message requireMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(messageId));
    }

    private void ensureSender(User user, Message message) {
        if (!message.getSender().getId().equals(user.getId())) {
            throw new MessageAccessDeniedException("Chỉ người gửi mới có thể thu hồi tin nhắn");
        }
    }

    private void touchConversation(Conversation conversation, Message message) {
        conversation.setLastMessageId(message.getId());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private void markSenderSeen(Conversation conversation, User sender, Message message) {
        conversationMemberRepository.findByConversation_IdAndUser_Id(conversation.getId(), sender.getId())
                .ifPresent(member -> {
                    member.setLastReadMessageId(message.getId());
                    member.setUpdatedAt(LocalDateTime.now());
                    conversationMemberRepository.save(member);
                });

        MessageSeenId id = new MessageSeenId(message.getId(), sender.getId());
        MessageSeen seen = MessageSeen.builder()
                .id(id)
                .message(message)
                .user(sender)
                .build();
        message.getSeen().add(seen);
    }

    private String normalizeContent(String value) {
        return value == null ? null : value.trim();
    }

    private MessageType resolveTypeFromFiles(List<MultipartFile> files) {
        if (files.size() != 1) {
            return MessageType.FILE;
        }

        String contentType = Optional.ofNullable(files.get(0).getContentType()).orElse("");
        if (contentType.startsWith("image/")) {
            return MessageType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MessageType.VIDEO;
        }
        if (contentType.startsWith("audio/")) {
            return MessageType.AUDIO;
        }
        return MessageType.FILE;
    }

    private boolean isImageContentType(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}
