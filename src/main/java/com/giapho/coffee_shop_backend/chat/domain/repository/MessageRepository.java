package com.giapho.coffee_shop_backend.chat.domain.repository;

import com.giapho.coffee_shop_backend.chat.domain.entity.Message;
import com.giapho.coffee_shop_backend.chat.domain.enums.MessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversation_IdAndStatusOrderByCreatedAtDesc(Long conversationId,
                                                                     MessageStatus status,
                                                                     Pageable pageable);

    List<Message> findByConversation_IdAndStatusAndCreatedAtLessThanOrderByCreatedAtDesc(Long conversationId,
                                                                                        MessageStatus status,
                                                                                        LocalDateTime createdAt,
                                                                                        Pageable pageable);

    long countByConversation_IdAndStatus(Long conversationId, MessageStatus status);

    long countByConversation_IdAndStatusAndCreatedAtLessThan(Long conversationId,
                                                             MessageStatus status,
                                                             LocalDateTime createdAt);

    long countByConversation_IdAndStatusAndIdGreaterThan(Long conversationId,
                                                         MessageStatus status,
                                                         Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Message m SET m.status = :status, m.content = :content, m.metadata = :metadata, m.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE m.id = :messageId")
    void updateStatus(@Param("messageId") Long messageId,
                      @Param("status") MessageStatus status,
                      @Param("content") String content,
                      @Param("metadata") String metadata);

    @Query("SELECT COUNT(m) > 0 FROM Message m " +
            "WHERE m.conversation.id = :conversationId AND m.sender.id = :senderId")
    boolean existsByConversationAndSender(@Param("conversationId") Long conversationId,
                                          @Param("senderId") Long senderId);
}
