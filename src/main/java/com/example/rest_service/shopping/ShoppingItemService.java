package com.example.rest_service.shopping;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShoppingItemService {

    private final ShoppingItemRepository repository;
    private final Clock clock;

    public ShoppingItemService(
            ShoppingItemRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<ShoppingItem> getItems() {
        return repository.findAllByOrderByUpdatedAtDesc();
    }

    public ShoppingItem getItem(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shopping item not found"));
    }

    @Transactional
    public ShoppingItem createItem(ShoppingItemRequest request) {
        ShoppingItem item = new ShoppingItem(
                request.productName().trim(),
                request.quantity(),
                Instant.now(clock));

        return repository.save(item);
    }

    @Transactional
    public ShoppingItem updateItem(
            long id,
            ShoppingItemRequest request) {
        ShoppingItem item = getItem(id);

        item.updateDetails(
                request.productName().trim(),
                request.quantity(),
                Instant.now(clock));

        return repository.save(item);
    }

    @Transactional
    public void deleteItem(long id) {
        ShoppingItem item = getItem(id);
        repository.delete(item);
    }
}