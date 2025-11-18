package com.giapho.coffee_shop_backend.chat.domain.repository;

import com.giapho.coffee_shop_backend.chat.domain.entity.Conversation;
import com.giapho.coffee_shop_backend.chat.domain.enums.ConversationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @EntityGraph(attributePaths = {"members", "members.user"})
    @Query(value = "SELECT DISTINCT c FROM Conversation c JOIN c.members m WHERE m.user.id = :userId ORDER BY c.updatedAt DESC",
            countQuery = "SELECT COUNT(DISTINCT c) FROM Conversation c JOIN c.members m WHERE m.user.id = :userId")
    Page<Conversation> findRecentByUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"members", "members.user"})
    @Query("SELECT c FROM Conversation c " +
            "JOIN c.members m1 " +
            "JOIN c.members m2 " +
            "WHERE c.type = :type AND m1.user.id = :userId AND m2.user.id = :otherUserId")
    Optional<Conversation> findDirectBetweenUsers(@Param("userId") Long userId,
                                                  @Param("otherUserId") Long otherUserId,
                                                  @Param("type") ConversationType type);

    @Query("SELECT COUNT(m) > 0 FROM Conversation c JOIN c.members m " +
            "WHERE c.id = :conversationId AND m.user.id = :userId")
    boolean existsByIdAndMember(@Param("conversationId") Long conversationId,
                                @Param("userId") Long userId);
}
