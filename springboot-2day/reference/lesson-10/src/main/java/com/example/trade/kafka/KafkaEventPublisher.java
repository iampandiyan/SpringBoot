package com.example.trade.kafka;

import com.example.trade.outbox.EventPublisher;
import com.example.trade.outbox.OutboxMessage;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// The Kafka transport for the outbox relay. Chosen with trade.events.transport=kafka (profile "kafka").
// The relay marks an outbox row published only when publish() RETURNS, so we wait here for the broker's acknowledgement.
// With acks=all that means every in-sync replica has the record: a "published" row is really safe.
@Component
@ConditionalOnProperty(name = "trade.events.transport", havingValue = "kafka")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafka;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(OutboxMessage message) {
        // The KEY is the aggregate (trade) id. Kafka sends equal keys to the same partition, and a partition is an
        // ordered log, so all events of ONE trade are consumed in the order they happened.
        ProducerRecord<String, String> record =
                new ProducerRecord<>(KafkaTopics.TRADE_EVENTS, message.aggregateId(), message.payload());
        record.headers().add(KafkaTopics.HEADER_EVENT_ID, String.valueOf(message.id()).getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaTopics.HEADER_EVENT_TYPE, message.type().getBytes(StandardCharsets.UTF_8));
        record.headers().add(KafkaTopics.HEADER_CREATED_AT, message.createdAt().toString().getBytes(StandardCharsets.UTF_8));
        try {
            kafka.send(record).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while publishing event " + message.id(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("kafka publish failed for event " + message.id(), e);   // the relay will retry
        }
    }
}
