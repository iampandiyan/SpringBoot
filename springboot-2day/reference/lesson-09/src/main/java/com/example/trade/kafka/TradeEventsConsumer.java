package com.example.trade.kafka;

import com.example.trade.outbox.OutboxMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

// The receiving end. Spring runs a listener container (a polling thread) that calls this method for every record.
// All it does is translate a Kafka record into our OutboxMessage and hand it to the idempotent processor.
@Component
@ConditionalOnProperty(name = "trade.events.transport", havingValue = "kafka")
public class TradeEventsConsumer {

    static final String CONSUMER_NAME = "trade-report";

    private final IdempotentEventProcessor processor;

    public TradeEventsConsumer(IdempotentEventProcessor processor) {
        this.processor = processor;
    }

    // groupId: consumers with the same group id SHARE the partitions (scale out); a different group id gets its own copy.
    @KafkaListener(topics = KafkaTopics.TRADE_EVENTS, groupId = "trade-report")
    public void onMessage(ConsumerRecord<String, String> record) {
        OutboxMessage message = new OutboxMessage(
                Long.valueOf(header(record, KafkaTopics.HEADER_EVENT_ID)),
                header(record, KafkaTopics.HEADER_EVENT_TYPE),
                record.key(),
                record.value(),
                Instant.parse(header(record, KafkaTopics.HEADER_CREATED_AT)));
        // An exception here is NOT swallowed: the error handler in KafkaConfig retries, then sends the record to the DLT.
        processor.process(CONSUMER_NAME, message);
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        Header h = record.headers().lastHeader(name);
        if (h == null) {
            throw new IllegalArgumentException("missing header " + name);     // a malformed record: straight to the DLT
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
