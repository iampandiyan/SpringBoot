package com.example.trade.kafka;

import com.example.trade.outbox.EventHandler;
import com.example.trade.outbox.OutboxMessage;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Makes at-least-once delivery harmless. In ONE database transaction it (1) checks whether this event was already
// processed, (2) runs the handlers, (3) records the event as processed. The handlers' writes and the "processed" row
// commit together, so there is no window in which one exists without the other.
@Component
public class IdempotentEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(IdempotentEventProcessor.class);

    private final ProcessedEventRepository processed;
    private final List<EventHandler> handlers;

    public IdempotentEventProcessor(ProcessedEventRepository processed, List<EventHandler> handlers) {
        this.processed = processed;
        this.handlers = handlers;
    }

    /** Returns true if the event was processed now, false if it was a duplicate and was skipped. */
    @Transactional
    public boolean process(String consumerName, OutboxMessage message) {
        String key = consumerName + ":" + message.id();
        if (processed.existsById(key)) {
            log.info("duplicate {} #{} skipped", message.type(), message.id());
            return false;
        }
        for (EventHandler handler : handlers) {
            if (handler.handles(message.type())) {
                handler.handle(message);
            }
        }
        // If two instances race on the same event, the second insert violates the primary key, its transaction
        // rolls back, Kafka redelivers, and the existsById check above then skips it.
        processed.save(new ProcessedEventEntity(key, Instant.now()));
        return true;
    }
}
