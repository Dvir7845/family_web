package com.familyhome.event;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    long deleteByEventDateBefore(LocalDate cutoff);
}
