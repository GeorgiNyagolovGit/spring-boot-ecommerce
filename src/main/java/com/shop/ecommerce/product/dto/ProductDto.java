package com.shop.ecommerce.product.dto;

import com.shop.ecommerce.product.entity.ProductCondition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProductDto {

        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stock;
        private String imageUrl;
        private Long categoryId;
        private String categoryName;
        private String location;
        private ProductCondition condition;
        private String contactPhone;
        private Long sellerId;
        private String sellerName;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ProductDto(
                Long id,
                String name,
                String description,
                BigDecimal price,
                Integer stock,
                String imageUrl,
                Long categoryId,
                String categoryName,
                LocalDateTime createdAt
        ) {
                this(id, name, description, price, stock, imageUrl,
                        categoryId, categoryName, null, null, null,
                        null, null, true, createdAt, null);
        }

        public ProductDto(
                Long id,
                String name,
                String description,
                BigDecimal price,
                Integer stock,
                String imageUrl,
                Long categoryId,
                String categoryName,
                String location,
                ProductCondition condition,
                String contactPhone,
                Long sellerId,
                String sellerName,
                boolean active,
                LocalDateTime createdAt,
                LocalDateTime updatedAt
        ) {
                this.id = id;
                this.name = name;
                this.description = description;
                this.price = price;
                this.stock = stock;
                this.imageUrl = imageUrl;
                this.categoryId = categoryId;
                this.categoryName = categoryName;
                this.location = location;
                this.condition = condition;
                this.contactPhone = contactPhone;
                this.sellerId = sellerId;
                this.sellerName = sellerName;
                this.active = active;
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
        }
}
