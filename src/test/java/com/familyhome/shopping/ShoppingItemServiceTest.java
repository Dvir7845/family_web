package com.familyhome.shopping;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        ShoppingItem item = service.createItem(
                new ShoppingItemRequest("Milk", 2));

        assertEquals("Milk", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(now, item.getUpdatedAt());

        verify(repository).save(item);
    }

    @Test
    void updatingAnOldItemRefreshesItsDate() {
        ShoppingItem item = new ShoppingItem(
                "Milk",
                1,
                now.minus(8, ChronoUnit.DAYS));

        when(repository.findById(1L))
                .thenReturn(Optional.of(item));

        when(repository.save(item)).thenReturn(item);

        ShoppingItem result = service.updateItem(
                1L,
                new ShoppingItemRequest("Milk", 3));

        assertEquals(3, result.getQuantity());
        assertEquals(now, result.getUpdatedAt());

        verify(repository).save(item);
    }

    @Test
    void viewingTheListDoesNotRefreshDates() {
        Instant oldDate = now.minus(6, ChronoUnit.DAYS);

        ShoppingItem item = new ShoppingItem(
                "Bread", 1, oldDate);

        when(repository.findAllByOrderByUpdatedAtDesc())
                .thenReturn(List.of(item));

        List<ShoppingItem> result = service.getItems();

        assertEquals(1, result.size());
        assertEquals(oldDate, result.get(0).getUpdatedAt());

        verify(repository, never()).save(any(ShoppingItem.class));
    }
}