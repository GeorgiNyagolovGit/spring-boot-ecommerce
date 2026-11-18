package com.shop.ecommerce.product.dto;

import jakarta.validation.constraints.NotNull;

public record ListingStatusRequest(
        @NotNull Boolean active
) {
}
