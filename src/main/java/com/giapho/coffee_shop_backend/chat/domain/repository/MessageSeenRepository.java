package com.giapho.coffee_shop_backend.chat.domain.repository;

import com.giapho.coffee_shop_backend.chat.domain.entity.MessageSeen;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageSeenId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageSeenRepository extends JpaRepository<MessageSeen, MessageSeenId> {

    List<MessageSeen> findByMessage_Conversation_Id(Long conversationId);

    List<MessageSeen> findByMessage_Id(Long messageId);
}
