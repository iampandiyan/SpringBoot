package com.example.trade.outbox;

// A consumer of events. Because delivery is at-least-once, handle() MUST be safe to call twice with the same message.
public interface EventHandler {
    boolean handles(String eventType);

    void handle(OutboxMessage message);
}
