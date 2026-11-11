package com.example.trade.outbox;

// How a message leaves the service. Today: in the same JVM. In lesson 09: a Kafka topic.
// Contract: throw a RuntimeException if the message could not be handed over; the relay will try again later.
public interface EventPublisher {
    void publish(OutboxMessage message);
}
