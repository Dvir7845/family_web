package com.familyhome.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shopping_items")
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ShoppingItem() {}

    public ShoppingItem(String productName, int quantity, Instant updatedAt) {
        this.productName = productName;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDetails(String productName, int quantity, Instant updatedAt) {
        this.productName = productName;
        this.quantity = quantity;
        this.updatedAt = updatedAt;
    }
}
