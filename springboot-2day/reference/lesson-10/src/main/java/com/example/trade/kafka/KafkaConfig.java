package com.example.trade.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
@ConditionalOnProperty(name = "trade.events.transport", havingValue = "kafka")
public class KafkaConfig {

    // Spring's KafkaAdmin creates these topics at startup if they are missing. In production, topics are normally
    // created by your platform team or infrastructure code, with the right partitions and replication.
    @Bean
    NewTopic tradeEventsTopic() {
        return TopicBuilder.name(KafkaTopics.TRADE_EVENTS)
                .partitions(3)          // 3 partitions: up to 3 consumers of one group work in parallel
                .replicas(1)            // 1 copy: fine for a laptop; production uses 3
                .build();
    }

    // The dead-letter topic must have at least as many partitions as the source: the recoverer writes to the SAME partition number.
    @Bean
    NewTopic tradeEventsDeadLetterTopic() {
        return TopicBuilder.name(KafkaTopics.TRADE_EVENTS_DLT).partitions(3).replicas(1).build();
    }

    // What happens when processing a record throws: retry a few times with growing pauses, then park the record
    // in "trade-events.DLT" and MOVE ON. Without this, one poison message would block its partition forever.
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(KafkaTopics.TRADE_EVENTS_DLT, record.partition()));
        var backOff = new ExponentialBackOffWithMaxRetries(3);      // 3 retries: 4 attempts in total
        backOff.setInitialInterval(200);                            // 200 ms, 400 ms, 800 ms (keep it short in a lesson;
        backOff.setMultiplier(2.0);                                 // production often uses seconds)
        backOff.setMaxInterval(2_000);
        var handler = new DefaultErrorHandler(recoverer, backOff);
        // A record that can never succeed (a missing header) is not worth retrying: straight to the DLT.
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
