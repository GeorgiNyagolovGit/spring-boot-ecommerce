package com.shop.ecommerce.messaging.repository;

import com.shop.ecommerce.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByListingIdAndBuyerId(
            Long listingId,
            Long buyerId
    );

    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.listing
            JOIN FETCH c.buyer
            JOIN FETCH c.seller
            WHERE c.buyer.id = :userId OR c.seller.id = :userId
            ORDER BY c.updatedAt DESC
            """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.listing
            JOIN FETCH c.buyer
            JOIN FETCH c.seller
            WHERE c.id = :conversationId
              AND (c.buyer.id = :userId OR c.seller.id = :userId)
            """)
    Optional<Conversation> findForParticipant(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );
}
