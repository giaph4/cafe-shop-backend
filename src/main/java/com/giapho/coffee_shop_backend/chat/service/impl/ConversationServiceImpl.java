package com.giapho.coffee_shop_backend.chat.service.impl;

import com.giapho.coffee_shop_backend.chat.domain.entity.Conversation;
import com.giapho.coffee_shop_backend.chat.domain.entity.ConversationMember;
import com.giapho.coffee_shop_backend.chat.domain.entity.ConversationMemberId;
import com.giapho.coffee_shop_backend.chat.domain.entity.Message;
import com.giapho.coffee_shop_backend.chat.domain.enums.ConversationMemberRole;
import com.giapho.coffee_shop_backend.chat.domain.enums.ConversationType;
import com.giapho.coffee_shop_backend.chat.domain.repository.ConversationMemberRepository;
import com.giapho.coffee_shop_backend.chat.domain.repository.ConversationRepository;
import com.giapho.coffee_shop_backend.chat.domain.repository.MessageRepository;
import com.giapho.coffee_shop_backend.chat.dto.ConversationMemberDTO;
import com.giapho.coffee_shop_backend.chat.dto.ConversationSummaryDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import com.giapho.coffee_shop_backend.chat.exception.ChatAccessDeniedException;
import com.giapho.coffee_shop_backend.chat.exception.ConversationNotFoundException;
import com.giapho.coffee_shop_backend.chat.mapper.ConversationMapper;
import com.giapho.coffee_shop_backend.chat.service.ConversationService;
import com.giapho.coffee_shop_backend.chat.util.ChatUserResolver;
import com.giapho.coffee_shop_backend.domain.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final ChatUserResolver chatUserResolver;

    @Override
    public Page<ConversationSummaryDTO> listUserConversations(Pageable pageable) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Page<Conversation> conversations = conversationRepository.findRecentByUser(currentUser.getId(), pageable);
        List<ConversationSummaryDTO> content = conversations.getContent().stream()
                .map(conversation -> toSummary(conversation, currentUser))
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, conversations.getTotalElements());
    }

    /**
     * Lấy thông tin cuộc trò chuyện
     *
     * @param conversationId ID của cuộc trò chuyện
     * @return Thông tin cuộc trò chuyện
     */
    @Override
    public ConversationSummaryDTO getConversation(Long conversationId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());
        return toSummary(conversation, currentUser);
    }

    /**
     * Tạo cuộc trò chuyện trực tiếp giữa người dùng hiện tại và người dùng đích
     *
     * @param targetUserId ID người dùng đích
     * @return Thông tin cuộc trò chuyện mới
     */
    @Override
    @Transactional
    public ConversationSummaryDTO createDirectConversation(Long targetUserId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        // Không thể tạo cuộc trò chuyện với chính mình
        if (currentUser.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("Không thể tạo cuộc trò chuyện với chính bạn");
        }

        // Kiểm tra xem cuộc trò chuyện đã tồn tại chưa
        Conversation existing = conversationRepository.findDirectBetweenUsers(
                currentUser.getId(), targetUserId, ConversationType.DIRECT).orElse(null);
        if (existing != null) {
            return toSummary(existing, currentUser);
        }

        // Tạo cuộc trò chuyện mới
        Conversation conversation = Conversation.builder()
                .type(ConversationType.DIRECT)
                .createdBy(currentUser.getId())
                .build();
        conversationRepository.save(conversation);

        addMember(conversation, currentUser, ConversationMemberRole.OWNER, false);
        // Thêm người dùng đích vào cuộc trò chuyện
        User targetUser = chatUserResolver.requireUser(targetUserId);
        addMember(conversation, targetUser, ConversationMemberRole.MEMBER, false);

        return toSummary(conversation, currentUser);
    }

    @Override
    @Transactional
    public ConversationSummaryDTO createGroupConversation(String title, List<Long> memberIds) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .title(title)
                .createdBy(currentUser.getId())
                .build();
        conversationRepository.save(conversation);

        addMember(conversation, currentUser, ConversationMemberRole.OWNER, false);
        if (memberIds != null) {
            memberIds.stream().distinct().forEach(memberId -> {
                if (!memberId.equals(currentUser.getId())) {
                    User member = chatUserResolver.requireUser(memberId);
                    addMember(conversation, member, ConversationMemberRole.MEMBER, false);
                }
            });
        }

        return toSummary(conversation, currentUser);
    }

    @Override
    public List<ConversationMemberDTO> listMembers(Long conversationId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());
        return conversationMapper.toMembers(conversation.getMembers());
    }

    @Override
    @Transactional
    public void addMembers(Long conversationId, List<Long> memberIds) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireOwnerConversation(conversationId, currentUser.getId());
        memberIds.stream().distinct().forEach(memberId -> {
            if (conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, memberId)) {
                return;
            }
            User member = chatUserResolver.requireUser(memberId);
            addMember(conversation, member, ConversationMemberRole.MEMBER, false);
        });
    }

    @Override
    @Transactional
    public void removeMember(Long conversationId, Long memberId) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireOwnerConversation(conversationId, currentUser.getId());
        if (memberId.equals(currentUser.getId())) {
            throw new IllegalArgumentException("Chủ nhóm không thể tự xoá");
        }
        ConversationMemberId id = new ConversationMemberId(conversationId, memberId);
        conversationMemberRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long conversationId, Long memberId, String role) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireOwnerConversation(conversationId, currentUser.getId());
        ConversationMemberRole targetRole = ConversationMemberRole.valueOf(role);
        conversationMemberRepository.updateRole(conversationId, memberId, targetRole);
    }

    @Override
    @Transactional
    public void pinConversation(Long conversationId, boolean pinned) {
        User currentUser = chatUserResolver.requireCurrentUser();
        if (!conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId())) {
            throw new ChatAccessDeniedException();
        }
        conversationMemberRepository.updatePinned(conversationId, currentUser.getId(), pinned);
    }

    @Override
    public Page<MessageDTO> listMessages(Long conversationId, Long beforeMessageId, Pageable pageable) {
        User currentUser = chatUserResolver.requireCurrentUser();
        if (!conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId())) {
            throw new ChatAccessDeniedException();
        }
        LocalDateTime cursorCreatedAt = null;
        if (beforeMessageId != null) {
            Message message = messageRepository.findById(beforeMessageId)
                    .orElseThrow(() -> new EntityNotFoundException("Message"));
            cursorCreatedAt = message.getCreatedAt();
        }
        List<Message> messages = cursorCreatedAt == null
                ? messageRepository.findByConversation_IdAndStatusOrderByCreatedAtDesc(
                conversationId,
                com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT,
                pageable)
                : messageRepository.findByConversation_IdAndStatusAndCreatedAtLessThanOrderByCreatedAtDesc(
                conversationId,
                com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT,
                cursorCreatedAt,
                pageable);
        List<MessageDTO> content = messages.stream()
                .map(message -> conversationMapper.toMessage(message, true))
                .collect(Collectors.toList());
        long total = beforeMessageId == null
                ? messageRepository.countByConversation_IdAndStatus(conversationId,
                com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT)
                : messageRepository.countByConversation_IdAndStatusAndCreatedAtLessThan(
                conversationId,
                com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT,
                cursorCreatedAt != null ? cursorCreatedAt : LocalDateTime.now());
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional
    public MessageDTO sendTextMessage(Long conversationId, String content) {
        User currentUser = chatUserResolver.requireCurrentUser();
        Conversation conversation = requireMemberConversation(conversationId, currentUser.getId());
        Message message = Message.builder()
                .conversation(conversation)
                .sender(currentUser)
                .content(content)
                .contentType(com.giapho.coffee_shop_backend.chat.domain.enums.MessageType.TEXT)
                .status(com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        messageRepository.save(message);
        conversation.setLastMessageId(message.getId());
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversationMapper.toMessage(message, true);
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

    private Conversation requireOwnerConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
        ConversationMember member = conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, userId)
                .orElseThrow(ChatAccessDeniedException::new);
        if (member.getRole() != ConversationMemberRole.OWNER) {
            throw new ChatAccessDeniedException("Chỉ chủ nhóm mới được thực hiện thao tác này");
        }
        return conversation;
    }

    private ConversationSummaryDTO toSummary(Conversation conversation, User currentUser) {
        Message lastMessage = null;
        if (conversation.getLastMessageId() != null) {
            lastMessage = messageRepository.findById(conversation.getLastMessageId()).orElse(null);
        }
        ConversationMember member = conversationMemberRepository.findByConversation_IdAndUser_Id(conversation.getId(), currentUser.getId())
                .orElseThrow(ChatAccessDeniedException::new);
        int unreadCount = calculateUnreadCount(conversation.getId(), member.getLastReadMessageId());
        return conversationMapper.toSummary(conversation, lastMessage, unreadCount, member.isPinned());
    }

    private int calculateUnreadCount(Long conversationId, Long lastReadMessageId) {
        if (lastReadMessageId == null) {
            return Math.toIntExact(messageRepository.countByConversation_IdAndStatus(
                    conversationId,
                    com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT
            ));
        }
        return Math.toIntExact(messageRepository.countByConversation_IdAndStatusAndIdGreaterThan(
                conversationId,
                com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus.SENT,
                lastReadMessageId
        ));
    }

    private void addMember(Conversation conversation,
                           User user,
                           ConversationMemberRole role,
                           boolean pinned) {
        ConversationMember member = ConversationMember.builder()
                .id(new ConversationMemberId(conversation.getId(), user.getId()))
                .conversation(conversation)
                .user(user)
                .role(role)
                .pinned(pinned)
                .muted(false)
                .joinedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ConversationMember managedMember = conversationMemberRepository.save(member);
        conversation.getMembers().add(managedMember);
    }
}
