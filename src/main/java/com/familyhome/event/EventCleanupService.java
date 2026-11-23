package com.familyhome.event;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventCleanupService {

    private final EventRepository repository;
    private final Clock clock;

    public EventCleanupService(EventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Jerusalem")
    public void cleanupOldEvents() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Jerusalem")));

        LocalDate cutoff = today.minusDays(30);
        repository.deleteByEventDateBefore(cutoff);
    }
}
