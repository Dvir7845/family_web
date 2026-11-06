package com.example.rest_service.shopping;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shopping-items")
public class ShoppingItemController {

    private final ShoppingItemService service;

    public ShoppingItemController(ShoppingItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShoppingItem> getItems() {
        return service.getItems();
    }

    @GetMapping("/{id}")
    public ShoppingItem getItem(@PathVariable long id) {
        return service.getItem(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingItem createItem(
            @Valid @RequestBody ShoppingItemRequest request) {
        return service.createItem(request);
    }

    @PutMapping("/{id}")
    public ShoppingItem updateItem(
            @PathVariable long id,
            @Valid @RequestBody ShoppingItemRequest request) {
        return service.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable long id) {
        service.deleteItem(id);
    }
}