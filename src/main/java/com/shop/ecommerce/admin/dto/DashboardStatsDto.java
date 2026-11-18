package com.shop.ecommerce.admin.dto;

public record DashboardStatsDto(
        long users,
        long products,
        long orders,
        long pendingOrders
) {
}
