package com.familyhome.event;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    long deleteByEventDateBefore(LocalDate cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM event_invitees WHERE user_id = :userId", nativeQuery = true)
    void deleteInviteeByUserId(@Param("userId") Long userId);
}
