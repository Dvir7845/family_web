package com.familyhome.shopping;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, Long> {

    List<ShoppingItem> findAllByOrderByUpdatedAtDesc(); 
    Page<ShoppingItem> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @Modifying
    @Query(
            """
            delete from ShoppingItem item
            where item.updatedAt <= :cutoff
            """)
    int deleteExpiredItems(@Param("cutoff") Instant cutoff);
}
