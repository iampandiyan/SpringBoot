package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.events.TradeSettledEvent;
import com.example.trade.outbox.DomainEvents;
import com.example.trade.outbox.EventPublisher;
import com.example.trade.outbox.OutboxEventEntity;
import com.example.trade.outbox.OutboxEventRepository;
import com.example.trade.outbox.OutboxMessage;
import com.example.trade.outbox.OutboxRelay;
import com.example.trade.service.TradeNotFoundException;
import com.example.trade.service.TradeService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.IllegalTransactionStateException;

// The real context and the real database. The relay's timer is set to one hour so that only OUR calls to
// publishPending() move events, and the publisher is replaced by a recording one that we can make fail.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "trade.outbox.poll-interval-ms=3600000")
class OutboxTest {

    static class RecordingPublisher implements EventPublisher {
        final List<OutboxMessage> sent = new CopyOnWriteArrayList<>();
        final AtomicInteger failures = new AtomicInteger();

        @Override
        public void publish(OutboxMessage message) {
            if (failures.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
                throw new IllegalStateException("broker unavailable");
            }
            sent.add(message);
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RecordingPublisher recordingPublisher() {
            return new RecordingPublisher();
        }
    }

    @Autowired TradeService trades;
    @Autowired OutboxEventRepository outbox;
    @Autowired OutboxRelay relay;
    @Autowired RecordingPublisher publisher;
    @Autowired DomainEvents domainEvents;

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        publisher.sent.clear();
        publisher.failures.set(0);
    }

    private Trade newTrade() {
        return trades.create("US0378331005", new BigDecimal("100"), new BigDecimal("189.50"));
    }

    private List<OutboxEventEntity> eventsOf(Long tradeId) {
        return outbox.findAll().stream().filter(e -> e.getAggregateId().equals(String.valueOf(tradeId))).toList();
    }

    @Test
    void settlingWritesTheEventInTheSameTransactionAsTheStatusChange() {
        Trade t = newTrade();
        trades.settle(t.id());

        List<OutboxEventEntity> events = eventsOf(t.id());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(TradeSettledEvent.TYPE);
        assertThat(events.get(0).getPayload()).contains("US0378331005");
        assertThat(events.get(0).getPublishedAt()).isNull();         // waiting for the relay
    }

    @Test
    void aRolledBackSettlementLeavesNoEventBehind() {
        Trade a = newTrade();

        // settleAll is all-or-nothing: the unknown id rolls back the whole transaction, including trade a ...
        assertThatThrownBy(() -> trades.settleAll(List.of(a.id(), 9999L))).isInstanceOf(TradeNotFoundException.class);

        // ... so the status change AND its event are both gone. The outbox cannot lie about what happened.
        assertThat(trades.get(a.id()).status()).isEqualTo(TradeStatus.NEW);
        assertThat(eventsOf(a.id())).isEmpty();
    }

    @Test
    void theRelayPublishesEachEventOnceAndMarksItPublished() {
        Trade t = newTrade();
        trades.settle(t.id());

        assertThat(relay.publishPending()).isEqualTo(1);
        assertThat(publisher.sent).hasSize(1);
        assertThat(publisher.sent.get(0).type()).isEqualTo(TradeSettledEvent.TYPE);
        assertThat(eventsOf(t.id()).get(0).getPublishedAt()).isNotNull();

        assertThat(relay.publishPending()).isZero();                   // nothing new, nothing re-sent
        assertThat(publisher.sent).hasSize(1);
    }

    @Test
    void aBrokerOutageLeavesTheEventForTheNextRun() {
        Trade t = newTrade();
        trades.settle(t.id());
        publisher.failures.set(1);

        assertThat(relay.publishPending()).isZero();                   // the publisher threw
        assertThat(eventsOf(t.id()).get(0).getPublishedAt()).isNull(); // still waiting: nothing is lost

        assertThat(relay.publishPending()).isEqualTo(1);               // the broker is back
        assertThat(publisher.sent).hasSize(1);
    }

    @Test
    void eventsLeaveInTheOrderTheyHappened() {
        Trade a = newTrade();
        Trade b = newTrade();
        trades.settle(b.id());
        trades.settle(a.id());

        relay.publishPending();

        assertThat(publisher.sent).extracting(OutboxMessage::aggregateId)
                .containsExactly(String.valueOf(b.id()), String.valueOf(a.id()));
    }

    @Test
    void recordingAnEventOutsideATransactionIsRefused() {
        assertThatThrownBy(() -> domainEvents.record("X", "1", "payload"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
