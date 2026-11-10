package com.familyhome.shopping;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingCleanupService {

    private final ShoppingItemRepository repository;
    private final Clock clock;

    public ShoppingCleanupService(
            ShoppingItemRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    @Scheduled(fixedDelay = 24, timeUnit = TimeUnit.HOURS)
    public void deleteExpiredItems() {
        Instant cutoff = Instant.now(clock)
                .minus(7, ChronoUnit.DAYS);

        repository.deleteExpiredItems(cutoff);
    }
}