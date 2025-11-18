package com.giapho.coffee_shop_backend.chat.domain.repository;

import com.giapho.coffee_shop_backend.chat.domain.entity.MessageDeletion;
import com.giapho.coffee_shop_backend.chat.domain.entity.MessageDeletionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageDeletionRepository extends JpaRepository<MessageDeletion, MessageDeletionId> {

    List<MessageDeletion> findByMessage_Conversation_IdAndUser_Id(Long conversationId, Long userId);
}
