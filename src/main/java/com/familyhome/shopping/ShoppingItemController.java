package com.familyhome.shopping;

import jakarta.validation.Valid;

 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public Page<ShoppingItemResponse> getItems(@PageableDefault(size = 50) Pageable pageable) {
        return service.getItems(pageable);
    }

    @GetMapping("/{id}")
    public ShoppingItemResponse getItem(@PathVariable long id) {
        return service.getItem(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingItemResponse createItem(@Valid @RequestBody ShoppingItemRequest request) {
        return service.createItem(request);
    }

    @PutMapping("/{id}")
    public ShoppingItemResponse updateItem(
            @PathVariable long id, @Valid @RequestBody ShoppingItemRequest request) {
        return service.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable long id) {
        service.deleteItem(id);
    }
}
