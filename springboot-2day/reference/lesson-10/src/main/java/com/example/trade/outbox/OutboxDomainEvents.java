package com.example.trade.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Writes the event into the outbox table. Propagation.MANDATORY means: "there MUST already be a transaction, join it".
// Called outside a transaction it fails loudly, which protects the one rule that makes the pattern work:
// the business change and its event are committed together, or neither is.
@Component
public class OutboxDomainEvents implements DomainEvents {

    private final OutboxEventRepository outbox;
    private final ObjectMapper json;

    public OutboxDomainEvents(OutboxEventRepository outbox, ObjectMapper json) {
        this.outbox = outbox;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, String aggregateId, Object payload) {
        try {
            outbox.save(new OutboxEventEntity(eventType, aggregateId, json.writeValueAsString(payload), Instant.now()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise event " + eventType, e);
        }
    }
}
