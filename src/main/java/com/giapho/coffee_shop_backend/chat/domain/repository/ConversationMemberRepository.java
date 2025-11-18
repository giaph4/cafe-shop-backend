package com.giapho.coffee_shop_backend.chat.domain.repository;

import com.giapho.coffee_shop_backend.chat.domain.entity.ConversationMember;
import com.giapho.coffee_shop_backend.chat.domain.entity.ConversationMemberId;
import com.giapho.coffee_shop_backend.chat.domain.enums.ConversationMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    List<ConversationMember> findByConversation_Id(Long conversationId);

    List<ConversationMember> findByConversation_IdAndUser_IdNot(Long conversationId, Long excludedUserId);

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    List<ConversationMember> findByConversation_IdAndPinnedTrueOrderByUpdatedAtDesc(Long conversationId);

    List<ConversationMember> findByUser_IdAndPinnedTrueOrderByUpdatedAtDesc(Long userId);

    Optional<ConversationMember> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationMember m SET m.pinned = :pinned, m.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE m.conversation.id = :conversationId AND m.user.id = :userId")
    void updatePinned(@Param("conversationId") Long conversationId,
                      @Param("userId") Long userId,
                      @Param("pinned") boolean pinned);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationMember m SET m.lastReadMessageId = :messageId, m.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE m.conversation.id IN :conversationIds AND m.user.id = :userId")
    void updateLastReadForConversations(@Param("conversationIds") Collection<Long> conversationIds,
                                        @Param("userId") Long userId,
                                        @Param("messageId") Long messageId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationMember m SET m.role = :role, m.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE m.conversation.id = :conversationId AND m.user.id = :userId")
    void updateRole(@Param("conversationId") Long conversationId,
                    @Param("userId") Long userId,
                    @Param("role") ConversationMemberRole role);
}
