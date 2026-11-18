package com.shop.ecommerce.product.dto;

import com.shop.ecommerce.product.entity.ProductCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ListingRequest(
        @NotBlank
        @Size(max = 150)
        @Schema(example = "iPhone 15 Pro 256 GB")
        String name,

        @NotBlank
        @Size(max = 2000)
        @Schema(example = "Excellent condition, with box and charger")
        String description,

        @NotNull
        @DecimalMin("0.00")
        @Schema(example = "1499.00")
        BigDecimal price,

        @Min(1)
        @Schema(example = "1")
        Integer stock,

        @Size(max = 500)
        @Schema(example = "https://example.com/iphone.jpg")
        String imageUrl,

        @NotNull
        Long categoryId,

        @NotBlank
        @Size(max = 120)
        @Schema(example = "Sofia")
        String location,

        @NotNull
        @Schema(example = "LIKE_NEW")
        ProductCondition condition,

        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^[+0-9() /-]{6,30}$")
        @Schema(example = "+359 888 123 456")
        String contactPhone
) {
}
