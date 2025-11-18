package com.giapho.coffee_shop_backend.chat.messaging;

import com.giapho.coffee_shop_backend.chat.dto.ConversationSummaryDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishNewMessage(MessageDTO message) {
        messagingTemplate.convertAndSend("/topic/conversations/" + message.getConversationId(), message);
    }

    public void publishConversationUpdate(ConversationSummaryDTO conversation) {
        messagingTemplate.convertAndSend("/topic/conversations", conversation);
    }

    public void publishMessageSeen(Long conversationId, Long messageId, Long userId) {
        messagingTemplate.convertAndSend(
                String.format("/topic/conversations/%d/seen", conversationId),
                new SeenEventPayload(messageId, userId)
        );
    }

    public record SeenEventPayload(Long messageId, Long userId) {
    }
}
