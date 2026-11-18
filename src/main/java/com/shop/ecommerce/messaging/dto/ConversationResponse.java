package com.shop.ecommerce.messaging.dto;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long listingId,
        String listingName,
        String listingImageUrl,
        Long otherUserId,
        String otherUserName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
