package com.example.rest_service.event;



import org.springframework.data.jpa.repository.JpaRepository;


public interface EventRepository extends JpaRepository<Event, Long> {

    long deleteByEventDateBefore(java.time.LocalDate cutoff);
}