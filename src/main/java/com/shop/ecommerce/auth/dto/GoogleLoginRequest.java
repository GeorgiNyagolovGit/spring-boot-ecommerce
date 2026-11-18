package com.shop.ecommerce.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotBlank
        @Size(max = 4096)
        @Schema(description = "Google Identity Services ID token")
        String credential
) {
}
