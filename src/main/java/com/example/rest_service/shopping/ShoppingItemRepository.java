package com.example.rest_service.shopping;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingItemRepository
        extends JpaRepository<ShoppingItem, Long> {

    List<ShoppingItem> findAllByOrderByUpdatedAtDesc();

    @Modifying
    @Query("""
            delete from ShoppingItem item
            where item.updatedAt <= :cutoff
            """)
    int deleteExpiredItems(@Param("cutoff") Instant cutoff);
}