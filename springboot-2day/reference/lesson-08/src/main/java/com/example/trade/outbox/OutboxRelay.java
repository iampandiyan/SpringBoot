package com.example.trade.outbox;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// The relay: a background loop that moves rows from the outbox to the publisher.
// It gives AT-LEAST-ONCE delivery: if we crash after publishing but before marking the row, the row is sent again.
// Consumers must therefore be idempotent (see EventHandler).
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final EventPublisher publisher;

    public OutboxRelay(OutboxEventRepository outbox, EventPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    // fixedDelay = wait this long AFTER the previous run ends, so two runs of one instance never overlap.
    @Scheduled(fixedDelayString = "${trade.outbox.poll-interval-ms:2000}")
    public void poll() {
        publishPending();
    }

    /** Publishes what is waiting, oldest first. Returns how many messages were sent. Public so tests can call it. */
    public int publishPending() {
        int sent = 0;
        for (OutboxEventEntity e : outbox.findTop50ByPublishedAtIsNullOrderByIdAsc()) {
            try {
                publisher.publish(new OutboxMessage(e.getId(), e.getEventType(), e.getAggregateId(), e.getPayload(), e.getCreatedAt()));
            } catch (RuntimeException ex) {
                // Stop at the first failure so later events never overtake an earlier one. The row stays unpublished.
                log.warn("publish of outbox event {} failed, will retry: {}", e.getId(), ex.toString());
                break;
            }
            outbox.markPublished(e.getId(), Instant.now());
            sent++;
        }
        return sent;
    }
}
