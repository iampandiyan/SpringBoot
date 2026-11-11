package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.trade.events.TradeSettledEvent;
import com.example.trade.kafka.KafkaEventPublisher;
import com.example.trade.kafka.KafkaTopics;
import com.example.trade.kafka.ProcessedEventRepository;
import com.example.trade.outbox.OutboxEventRepository;
import com.example.trade.outbox.OutboxMessage;
import com.example.trade.outbox.OutboxRelay;
import com.example.trade.report.SettledTradeReportRepository;
import com.example.trade.service.TradeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

// A REAL Kafka broker runs inside the test JVM (@EmbeddedKafka), no Docker needed. The "kafka" profile switches the
// outbox relay from the in-process transport to Kafka, and the @KafkaListener consumer starts.
@SpringBootTest(properties = "trade.outbox.poll-interval-ms=3600000")
@ActiveProfiles({"dev", "kafka"})
@EmbeddedKafka(partitions = 3, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class KafkaFlowTest {

    // Every test uses its OWN instrument code. Events arrive asynchronously, so a late event of one test must never
    // be visible to the assertions of the next one.
    private static final String ISIN_FLOW = "US0000000001";
    private static final String ISIN_DUPLICATE = "US0000000002";
    private static final String ISIN_POISON = "US0000000003";
    private static final String ISIN_KEYS = "US0000000004";

    @Autowired TradeService trades;
    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired KafkaEventPublisher publisher;
    @Autowired SettledTradeReportRepository reports;
    @Autowired ProcessedEventRepository processed;
    @Autowired EmbeddedKafkaBroker broker;
    @Autowired ObjectMapper json;

    private OutboxMessage settledMessage(long outboxId, long tradeId, String isin) throws Exception {
        String payload = json.writeValueAsString(
                new TradeSettledEvent(tradeId, isin, new BigDecimal("10"), new BigDecimal("2.5"), Instant.now()));
        return new OutboxMessage(outboxId, TradeSettledEvent.TYPE, String.valueOf(tradeId), payload, Instant.now());
    }

    // A plain Kafka consumer in its own group, reading a topic from the beginning: we look at what is REALLY on the topic.
    private List<ConsumerRecord<String, String>> readAll(String topic, Duration wait) {
        Map<String, Object> props = KafkaTestUtils.consumerProps("test-" + System.nanoTime(), "false", broker);
        props.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> c = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer()) {
            c.subscribe(List.of(topic));                       // join a fresh group and read from the start
            List<ConsumerRecord<String, String>> all = new ArrayList<>();
            long end = System.currentTimeMillis() + wait.toMillis();
            while (System.currentTimeMillis() < end) {
                KafkaTestUtils.getRecords(c, Duration.ofMillis(500)).forEach(all::add);
            }
            return all;
        }
    }

    private static String header(ConsumerRecord<String, String> r, String name) {
        var h = r.headers().lastHeader(name);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    @Test
    void anEventTravelsFromTheOutboxThroughKafkaIntoTheReadModel() {
        var t = trades.create(ISIN_FLOW, new BigDecimal("100"), new BigDecimal("2"));
        trades.settle(t.id());

        relay.publishPending();                                  // outbox -> Kafka topic

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->   // Kafka -> consumer -> projector -> report table
                assertThat(reports.findByIsinOrderBySettledAtDesc(ISIN_FLOW)).extracting("tradeId").contains(t.id()));
    }

    @Test
    void theSameEventDeliveredTwiceIsProcessedOnce() throws Exception {
        OutboxMessage a = settledMessage(900_001, 900_001, ISIN_DUPLICATE);
        OutboxMessage sentinel = settledMessage(900_002, 900_001, ISIN_DUPLICATE);   // same key => same partition => read AFTER both copies of a

        publisher.publish(a);
        publisher.publish(a);                                        // a duplicate, as at-least-once delivery allows
        publisher.publish(sentinel);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(processed.existsById("trade-report:900002")).isTrue());   // the sentinel was handled, so both copies of a were seen
        assertThat(processed.existsById("trade-report:900001")).isTrue();
        assertThat(reports.findByIsinOrderBySettledAtDesc(ISIN_DUPLICATE)).hasSize(1); // one row, not two
    }

    @Test
    void aPoisonRecordIsRetriedThenParkedInTheDeadLetterTopic() throws Exception {
        OutboxMessage poison = new OutboxMessage(900_010L, TradeSettledEvent.TYPE, "900010", "this is not json", Instant.now());

        publisher.publish(poison);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<ConsumerRecord<String, String>> dead = readAll(KafkaTopics.TRADE_EVENTS_DLT, Duration.ofSeconds(1));
            assertThat(dead).anySatisfy(r -> {
                assertThat(r.key()).isEqualTo("900010");
                assertThat(r.value()).isEqualTo("this is not json");
                assertThat(header(r, "kafka_dlt-exception-message")).contains("900010");   // why it was parked
            });
        });
        assertThat(processed.existsById("trade-report:900010")).isFalse();     // never marked as processed
        assertThat(reports.findByIsinOrderBySettledAtDesc(ISIN_POISON)).isEmpty();    // and nothing leaked into the read model
    }

    @Test
    void recordsCarryTheTradeIdAsKeyAndTheEventTypeAsHeader() throws Exception {
        publisher.publish(settledMessage(900_020, 900_020, ISIN_KEYS));
        publisher.publish(settledMessage(900_021, 900_020, ISIN_KEYS));        // another event of the same trade

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<ConsumerRecord<String, String>> sent = readAll(KafkaTopics.TRADE_EVENTS, Duration.ofSeconds(1)).stream()
                    .filter(r -> "900020".equals(r.key())).toList();
            assertThat(sent).hasSize(2);
            assertThat(header(sent.get(0), KafkaTopics.HEADER_EVENT_TYPE)).isEqualTo("TradeSettled");
            assertThat(header(sent.get(0), KafkaTopics.HEADER_EVENT_ID)).isEqualTo("900020");
            assertThat(sent.get(0).partition()).isEqualTo(sent.get(1).partition());   // same key, same partition: order kept
        });
    }
}
