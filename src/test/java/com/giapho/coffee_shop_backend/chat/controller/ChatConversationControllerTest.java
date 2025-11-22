package com.giapho.coffee_shop_backend.chat.controller;

import com.giapho.coffee_shop_backend.chat.domain.enums.ConversationMemberRole;
import com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus;
import com.giapho.coffee_shop_backend.chat.domain.enums.MessageType;
import com.giapho.coffee_shop_backend.chat.dto.ConversationMemberDTO;
import com.giapho.coffee_shop_backend.chat.dto.ConversationSummaryDTO;
import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import com.giapho.coffee_shop_backend.chat.messaging.ChatMessageWebSocketPublisher;
import com.giapho.coffee_shop_backend.chat.service.ConversationService;
import com.giapho.coffee_shop_backend.chat.service.MessageService;
import com.giapho.coffee_shop_backend.security.CustomAccessDeniedHandler;
import com.giapho.coffee_shop_backend.security.JwtAuthenticationFilter;
import com.giapho.coffee_shop_backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatConversationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private ChatMessageWebSocketPublisher publisher;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @MockitoBean
    private CustomAccessDeniedHandler accessDeniedHandler;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void sendText_shouldReturnMessageAndEmitEvents() throws Exception {
        MessageDTO message = MessageDTO.builder()
                .id(10L)
                .conversationId(1L)
                .senderId(5L)
                .senderName("Nguyen Van A")
                .senderAvatar("https://cdn/avatar.png")
                .content("Xin chào")
                .contentType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .metadata(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .attachments(Collections.emptyList())
                .seenByUserIds(Collections.emptyList())
                .build();
        when(messageService.sendText(1L, "Xin chào")).thenReturn(message);

        ConversationSummaryDTO summary = ConversationSummaryDTO.builder()
                .id(1L)
                .title("Nhóm test")
                .avatarUrl(null)
                .type("GROUP")
                .updatedAt(LocalDateTime.now())
                .lastMessage(message)
                .unreadCount(0)
                .pinned(false)
                .participants(Collections.singletonList(ConversationMemberDTO.builder()
                        .userId(5L)
                        .fullName("Nguyen Van A")
                        .avatarUrl(null)
                        .role(ConversationMemberRole.OWNER)
                        .pinned(false)
                        .muted(false)
                        .lastReadMessageId(10L)
                        .build()))
                .build();
        when(conversationService.getConversation(1L)).thenReturn(summary);

        mockMvc.perform(post("/api/chat/conversations/{conversationId}/messages/text", 1L)
                        .param("content", "Xin chào")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.content").value("Xin chào"));

        verify(messageService).sendText(1L, "Xin chào");
        verify(publisher).publishNewMessage(message);
        verify(publisher).publishConversationUpdate(summary);
    }

    @Test
    void markSeen_shouldReturnNoContentAndPublishSeenEvent() throws Exception {
        when(messageService.markSeen(1L, 2L)).thenReturn(99L);

        mockMvc.perform(post("/api/chat/conversations/{conversationId}/messages/{messageId}/seen", 1L, 2L))
                .andExpect(status().isNoContent());

        verify(messageService).markSeen(1L, 2L);

        ArgumentCaptor<Long> convoCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> messageCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);

        verify(publisher).publishMessageSeen(convoCaptor.capture(), messageCaptor.capture(), userCaptor.capture());

        assertThat(convoCaptor.getValue()).isEqualTo(1L);
        assertThat(messageCaptor.getValue()).isEqualTo(2L);
        assertThat(userCaptor.getValue()).isEqualTo(99L);
    }

}
