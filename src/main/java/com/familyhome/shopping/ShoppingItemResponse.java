package com.familyhome.shopping;

import java.time.Instant;

public record ShoppingItemResponse(Long id, String productName, int quantity, Instant updatedAt) {
    public static ShoppingItemResponse from(ShoppingItem item) {
        return new ShoppingItemResponse(
            item.getId(), item.getProductName(), item.getQuantity(), item.getUpdatedAt());
    }
}
