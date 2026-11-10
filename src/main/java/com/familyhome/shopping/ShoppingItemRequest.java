package com.familyhome.shopping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ShoppingItemRequest(
    @NotBlank(message = "Product name is required")
    String productName,

    @Positive(message = "Quantity must be greater than zero")
    int quantity
) {}