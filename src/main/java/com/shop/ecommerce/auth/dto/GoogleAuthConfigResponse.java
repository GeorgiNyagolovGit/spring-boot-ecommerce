package com.shop.ecommerce.auth.dto;

public record GoogleAuthConfigResponse(
        String clientId,
        boolean enabled
) {
}
