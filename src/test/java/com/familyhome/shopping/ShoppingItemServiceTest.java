package com.familyhome.shopping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ShoppingItemServiceTest {

    private final Instant now = Instant.parse("2026-11-06T12:00:00Z");

    private ShoppingItemRepository repository;
    private ShoppingItemService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShoppingItemRepository.class);

        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        service = new ShoppingItemService(repository, clock);
    }

    @Test
    void creationUsesServerTime() {
        when(repository.save(any(ShoppingItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingItemResponse item = service.createItem(new ShoppingItemRequest("Milk", 2));

        assertEquals("Milk", item.productName());
        assertEquals(2, item.quantity());
        assertEquals(now, item.updatedAt());

        verify(repository).save(any(ShoppingItem.class));
    }

    @Test
    void updatingAnOldItemRefreshesItsDate() {
        ShoppingItem item = new ShoppingItem("Milk", 1, now.minus(8, ChronoUnit.DAYS));

        when(repository.findById(1L)).thenReturn(Optional.of(item));

        when(repository.save(item)).thenReturn(item);

        ShoppingItemResponse result = service.updateItem(1L, new ShoppingItemRequest("Milk", 3));

        assertEquals(3, result.quantity());
        assertEquals(now, result.updatedAt());

        verify(repository).save(item);
    }

    @Test
    void viewingTheListDoesNotRefreshDates() {
        Instant oldDate = now.minus(6, ChronoUnit.DAYS);

        ShoppingItem item = new ShoppingItem("Bread", 1, oldDate);
        Pageable pageable = PageRequest.of(0, 20);

        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(item)));

        Page<ShoppingItemResponse> result = service.getItems(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(oldDate, result.getContent().get(0).updatedAt());

        verify(repository, never()).save(any(ShoppingItem.class));
    }
}
