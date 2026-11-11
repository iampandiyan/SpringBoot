package com.example.trade.outbox;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    // Oldest first: events of one trade leave in the order they happened. A batch keeps each poll short.
    List<OutboxEventEntity> findTop50ByPublishedAtIsNullOrderByIdAsc();

    long countByPublishedAtIsNull();

    // A single UPDATE in its own tiny transaction. The relay never keeps a transaction open while it talks to the broker.
    @Transactional
    @Modifying
    @Query("update OutboxEventEntity e set e.publishedAt = :at where e.id = :id")
    int markPublished(@Param("id") Long id, @Param("at") Instant at);
}
