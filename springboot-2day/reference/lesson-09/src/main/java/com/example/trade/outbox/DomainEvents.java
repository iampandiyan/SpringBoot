package com.example.trade.outbox;

// What the business code sees: "record that this happened". It does not know about tables, relays or brokers.
// Unit tests pass a one-line lambda; the real implementation writes to the outbox table.
public interface DomainEvents {
    void record(String eventType, String aggregateId, Object payload);
}
