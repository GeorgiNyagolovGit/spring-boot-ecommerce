package com.shop.ecommerce.messaging.repository;

import com.shop.ecommerce.messaging.entity.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversationIdOrderByCreatedAtAsc(
            Long conversationId
    );

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(
            Long conversationId
    );

    long countByConversationIdAndSenderIdNotAndReadAtIsNull(
            Long conversationId,
            Long senderId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Message m
            SET m.readAt = :readAt
            WHERE m.conversation.id = :conversationId
              AND m.sender.id <> :readerId
              AND m.readAt IS NULL
            """)
    int markAsRead(
            @Param("conversationId") Long conversationId,
            @Param("readerId") Long readerId,
            @Param("readAt") LocalDateTime readAt
    );
}
