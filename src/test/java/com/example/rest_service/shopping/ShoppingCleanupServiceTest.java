package com.example.rest_service.shopping;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true"
})
class ShoppingCleanupServiceTest {

    @Autowired
    private ShoppingItemRepository repository;

    @Test
    void deletesOldItemsButKeepsRecentlyUpdatedItems() {
        Instant now = Instant.parse("2026-11-06T12:00:00Z");

        ShoppingItem expired = new ShoppingItem(
                "Old milk", 1, now.minus(8, ChronoUnit.DAYS));

        ShoppingItem exactlyOneWeekOld = new ShoppingItem(
                "Old bread", 1, now.minus(7, ChronoUnit.DAYS));

        ShoppingItem recent = new ShoppingItem(
                "Eggs", 2, now.minus(6, ChronoUnit.DAYS));

        ShoppingItem updated = new ShoppingItem(
                "Rice", 1, now.minus(20, ChronoUnit.DAYS));

        // Update the "Rice" item to have a recent updatedAt timestamp, so it should not
        // be deleted
        updated.updateDetails(
                "Rice", 3, now.minus(1, ChronoUnit.DAYS));

        repository.saveAllAndFlush(List.of(
                expired, exactlyOneWeekOld, recent, updated));

        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);

        ShoppingCleanupService cleanup = new ShoppingCleanupService(repository, fixedClock);

        cleanup.deleteExpiredItems();

        Set<Long> remainingIds = repository.findAll()
                .stream()
                .map(ShoppingItem::getId)
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(recent.getId(), updated.getId()),
                remainingIds);
    }
}