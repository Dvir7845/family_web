package com.familyhome.shopping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ShoppingItemRequest(
        @NotBlank(message = "Product name is required")
                @Size(max = 100, message = "Product name must be at most 100 characters")
                String productName,
        @Positive(message = "Quantity must be greater than zero") int quantity) {}
