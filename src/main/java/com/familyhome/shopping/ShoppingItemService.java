package com.familyhome.shopping;

import java.time.Clock;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import com.familyhome.exception.ResourceNotFoundException;

@Service
public class ShoppingItemService {

    private final ShoppingItemRepository repository;
    private final Clock clock;

    public ShoppingItemService(ShoppingItemRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Page<ShoppingItemResponse> getItems(Pageable pageable) {
        return repository.findAll(pageable).map(ShoppingItemResponse::from);
    }

    public ShoppingItemResponse getItem(long id) {
        return repository
                .findById(id)
                .map(ShoppingItemResponse::from)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Shopping item not found"));
    }

    @Transactional
    public ShoppingItemResponse createItem(ShoppingItemRequest request) {
        ShoppingItem item =
                new ShoppingItem(
                        request.productName().trim(), request.quantity(), Instant.now(clock));

        return ShoppingItemResponse.from(repository.save(item));
    }

    @Transactional
    public ShoppingItemResponse updateItem(long id, ShoppingItemRequest request) {
        ShoppingItem item = repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Shopping item not found"));

        item.updateDetails(request.productName().trim(), request.quantity(), Instant.now(clock));

        return ShoppingItemResponse.from(repository.save(item));
    }

    @Transactional
    public void deleteItem(long id) {
        ShoppingItemResponse item = getItem(id);
        repository.deleteById(item.id());
    }
}
