package com.familyhome.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class EventCleanupServiceTest {

    @Autowired private EventRepository repository;

    @Test
    void cleanupUsesIsraelDateAndKeepsBoundaryAndFutureEvents() {
        // In Israel this instant is already November 9.
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T22:30:00Z"), ZoneOffset.UTC);

        // November 9 minus 30 days is October 10.
        Event expired = new Event("Expired event", LocalDate.of(2026, 10, 9), Set.of());

        Event boundary = new Event("Exactly 30 days ago", LocalDate.of(2026, 10, 10), Set.of());

        Event recent = new Event("Recent event", LocalDate.of(2026, 11, 8), Set.of());

        Event future = new Event("Future event", LocalDate.of(2026, 12, 1), Set.of());

        repository.saveAllAndFlush(List.of(expired, boundary, recent, future));

        EventCleanupService cleanup = new EventCleanupService(repository, clock);

        cleanup.cleanupOldEvents();
        repository.flush();

        Set<Long> remainingIds =
                repository.findAll().stream().map(Event::getId).collect(Collectors.toSet());

        assertEquals(Set.of(boundary.getId(), recent.getId(), future.getId()), remainingIds);
    }
}
