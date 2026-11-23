package com.familyhome.shopping;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    Page<ShoppingItem> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @Modifying
    @Query(
            """
            delete from ShoppingItem item
            where item.updatedAt <= :cutoff
            """)
    int deleteExpiredItems(@Param("cutoff") Instant cutoff);
}
