package com.familyhome.event;



import org.springframework.data.jpa.repository.JpaRepository;


public interface EventRepository extends JpaRepository<Event, Long> {

    long deleteByEventDateBefore(java.time.LocalDate cutoff);
}