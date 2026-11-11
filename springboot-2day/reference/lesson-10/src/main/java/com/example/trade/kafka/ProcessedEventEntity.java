package com.example.trade.kafka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// "I have already handled this event." The id is "<consumer name>:<event id>", so two different consumers
// (a report builder, an e-mail sender) each keep their own memory of the same event.
@Entity
@Table(name = "processed_event")
public class ProcessedEventEntity {

    @Id
    @Column(length = 80)
    private String id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEventEntity() {}

    public ProcessedEventEntity(String id, Instant processedAt) {
        this.id = id;
        this.processedAt = processedAt;
    }

    public String getId() { return id; }

    public Instant getProcessedAt() { return processedAt; }
}
