package com.example.trade.outbox;

import java.time.Instant;

// What leaves the outbox. The id is the idempotency key for consumers: the same message may arrive more than once.
public record OutboxMessage(Long id, String type, String aggregateId, String payload, Instant createdAt) {}
