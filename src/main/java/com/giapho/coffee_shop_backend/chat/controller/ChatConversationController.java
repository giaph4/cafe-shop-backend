package com.giapho.coffee_shop_backend.chat.controller;

import com.giapho.coffee_shop_backend.chat.dto.ConversationMemberDTO;
import com.giapho.coffee_shop_backend.chat.dto.ConversationSummaryDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import com.giapho.coffee_shop_backend.chat.dto.request.CreateGroupConversationRequest;
import com.giapho.coffee_shop_backend.chat.dto.request.SendAttachmentMessageRequest;
import com.giapho.coffee_shop_backend.chat.messaging.ChatMessageWebSocketPublisher;
import com.giapho.coffee_shop_backend.chat.service.ConversationService;
import com.giapho.coffee_shop_backend.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Validated
public class ChatConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatMessageWebSocketPublisher publisher;

    /**
     * Lấy danh sách hội thoại của người dùng (phân trang)
     *
     * @param page trang hiện tại, mặc định 0
     * @param size số lượng hội thoại trên mỗi trang, mặc định 20, giới hạn 1-100
     * @return trang danh sách hội thoại của người dùng
     */
    @GetMapping("/conversations")
    public Page<ConversationSummaryDTO> listConversations(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return conversationService.listUserConversations(pageable);
    }

    @GetMapping("/conversations/{conversationId}")
    public ConversationSummaryDTO getConversation(@PathVariable Long conversationId) {
        return conversationService.getConversation(conversationId);
    }

    @PostMapping("/conversations/direct/{targetUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationSummaryDTO createDirect(@PathVariable Long targetUserId) {
        ConversationSummaryDTO summary = conversationService.createDirectConversation(targetUserId);
        publisher.publishConversationUpdate(summary);
        return summary;
    }

    @PostMapping("/conversations/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationSummaryDTO createGroup(
            @RequestBody @Valid CreateGroupConversationRequest request
    ) {
        ConversationSummaryDTO summary = conversationService.createGroupConversation(request.title(), request.memberIds());
        publisher.publishConversationUpdate(summary);
        return summary;
    }

    @GetMapping("/conversations/{conversationId}/members")
    public List<ConversationMemberDTO> listMembers(@PathVariable Long conversationId) {
        return conversationService.listMembers(conversationId);
    }

    @PostMapping("/conversations/{conversationId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMembers(
            @PathVariable Long conversationId,
            @RequestBody @NotEmpty @Size(max = 100) List<@NotNull Long> memberIds
    ) {
        conversationService.addMembers(conversationId, memberIds);
    }

    @DeleteMapping("/conversations/{conversationId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long conversationId,
            @PathVariable Long memberId
    ) {
        conversationService.removeMember(conversationId, memberId);
    }

    @PatchMapping("/conversations/{conversationId}/members/{memberId}/role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRole(
            @PathVariable Long conversationId,
            @PathVariable Long memberId,
            @RequestParam @NotBlank String role
    ) {
        conversationService.updateMemberRole(conversationId, memberId, role);
    }

    @PatchMapping("/conversations/{conversationId}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pinConversation(@PathVariable Long conversationId, @RequestParam boolean pinned) {
        conversationService.pinConversation(conversationId, pinned);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Page<MessageDTO> listMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return conversationService.listMessages(conversationId, beforeMessageId, pageable);
    }

    @PostMapping("/conversations/{conversationId}/messages/text")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO sendText(@PathVariable Long conversationId, @RequestParam @NotBlank String content) {
        MessageDTO message = messageService.sendText(conversationId, content);
        publisher.publishNewMessage(message);
        publisher.publishConversationUpdate(conversationService.getConversation(conversationId));
        return message;
    }

    @PostMapping("/conversations/{conversationId}/messages/emoji")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO sendEmoji(@PathVariable Long conversationId, @RequestParam("code") @NotBlank String code) {
        MessageDTO message = messageService.sendEmoji(conversationId, code);
        publisher.publishNewMessage(message);
        publisher.publishConversationUpdate(conversationService.getConversation(conversationId));
        return message;
    }

    @PostMapping("/conversations/{conversationId}/messages/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDTO sendAttachment(
            @PathVariable Long conversationId,
            @Valid @ModelAttribute SendAttachmentMessageRequest request
    ) {
        MessageDTO message = messageService.sendAttachments(conversationId, request.messageText(), request.files());
        publisher.publishNewMessage(message);
        publisher.publishConversationUpdate(conversationService.getConversation(conversationId));
        return message;
    }

    @PostMapping("/messages/{messageId}/recall")
    public MessageDTO recall(@PathVariable Long messageId) {
        MessageDTO message = messageService.recall(messageId);
        publisher.publishNewMessage(message);
        publisher.publishConversationUpdate(conversationService.getConversation(message.getConversationId()));
        return message;
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteForCurrentUser(@PathVariable Long messageId) {
        messageService.deleteForCurrentUser(messageId);
    }

    @PostMapping("/conversations/{conversationId}/messages/{messageId}/seen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markSeen(
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ) {
        Long userId = messageService.markSeen(conversationId, messageId);
        publisher.publishMessageSeen(conversationId, messageId, userId);
    }
}
