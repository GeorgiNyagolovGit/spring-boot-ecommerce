package com.shop.ecommerce.messaging.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String body,
        boolean sentByMe,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
