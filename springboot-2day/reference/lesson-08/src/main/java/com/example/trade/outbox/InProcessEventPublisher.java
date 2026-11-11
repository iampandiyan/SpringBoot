package com.example.trade.outbox;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// The simplest transport: hand the message to every handler that wants it, inside this JVM.
// It lets us build and test the whole pattern before a broker exists.
@Component
public class InProcessEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InProcessEventPublisher.class);

    private final List<EventHandler> handlers;

    public InProcessEventPublisher(List<EventHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void publish(OutboxMessage message) {
        log.info("publish {} #{} for aggregate {}", message.type(), message.id(), message.aggregateId());
        for (EventHandler handler : handlers) {
            if (handler.handles(message.type())) {
                handler.handle(message);
            }
        }
    }
}
