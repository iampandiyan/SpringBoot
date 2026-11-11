package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.events.TradeSettledEvent;
import com.example.trade.events.TradeSettlementFailedEvent;
import com.example.trade.gateway.FlakySettlementGateway;
import com.example.trade.outbox.OutboxEventEntity;
import com.example.trade.outbox.OutboxEventRepository;
import com.example.trade.saga.CashLedger;
import com.example.trade.saga.ConfirmWithGatewayStep;
import com.example.trade.saga.SagaInstanceRepository;
import com.example.trade.saga.SagaJournal;
import com.example.trade.saga.SagaState;
import com.example.trade.saga.SecuritiesDepot;
import com.example.trade.saga.SettlementOutcome;
import com.example.trade.saga.SettlementSaga;
import com.example.trade.service.TradeService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

// The saga with every collaborator real. The "other services" (cash ledger, depot, gateway) are in-process stand-ins
// that we can tell to fail, so we can watch the compensation happen.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "trade.outbox.poll-interval-ms=3600000",
        "resilience4j.retry.instances.gateway.wait-duration=10ms"})
class SagaTest {

    @Autowired SettlementSaga saga;
    @Autowired TradeService trades;
    @Autowired SagaJournal journal;
    @Autowired CashLedger ledger;
    @Autowired SecuritiesDepot depot;
    @Autowired FlakySettlementGateway gateway;
    @Autowired CircuitBreakerRegistry breakers;
    @Autowired OutboxEventRepository outbox;
    @Autowired SagaInstanceRepository sagas;
    @MockitoSpyBean ConfirmWithGatewayStep gatewayStep;      // lets us simulate "the process died during this step"

    @BeforeEach
    @AfterEach
    void clean() {
        journal.clear();
        ledger.reset();
        depot.reset();
        gateway.reset();
        breakers.circuitBreaker("gateway").reset();
        outbox.deleteAll();
        reset(gatewayStep);
    }

    private Trade newTrade() {
        return trades.create("US0378331005", new BigDecimal("100"), new BigDecimal("189.50"));
    }

    private List<String> eventTypesOf(Long tradeId) {
        return outbox.findAll().stream().filter(e -> e.getAggregateId().equals(String.valueOf(tradeId)))
                .map(OutboxEventEntity::getEventType).toList();
    }

    @Test
    void whenEveryStepSucceedsTheTradeIsSettledAndTheWorldIsTold() {
        Trade t = newTrade();

        SettlementOutcome outcome = saga.run(t.id());

        assertThat(outcome.state()).isEqualTo(SagaState.COMPLETED);
        assertThat(trades.get(t.id()).status()).isEqualTo(TradeStatus.SETTLED);
        assertThat(journal.entries()).containsExactly(
                "reserve-cash " + t.id(), "deliver-securities " + t.id(), "confirm-with-gateway " + t.id());
        assertThat(ledger.isReserved(t.id())).isTrue();
        assertThat(depot.isDelivered(t.id())).isTrue();
        assertThat(eventTypesOf(t.id())).containsExactly(TradeSettledEvent.TYPE);
    }

    @Test
    void whenTheDepotRefusesTheCashReservationIsUndone() {
        Trade t = newTrade();
        depot.refuseDeliveries(true);

        SettlementOutcome outcome = saga.run(t.id());

        assertThat(outcome.state()).isEqualTo(SagaState.COMPENSATED);
        assertThat(outcome.failureReason()).contains("deliver-securities").contains("depot refused");
        assertThat(journal.entries()).containsExactly(
                "reserve-cash " + t.id(), "undo reserve-cash " + t.id(), "saga " + outcome.sagaId() + " compensated");
        assertThat(ledger.isReserved(t.id())).isFalse();
        assertThat(trades.get(t.id()).status()).isEqualTo(TradeStatus.FAILED);
        assertThat(eventTypesOf(t.id())).containsExactly(TradeSettlementFailedEvent.TYPE);   // and NOT TradeSettled
    }

    @Test
    void whenTheGatewayIsDownEarlierStepsAreUndoneInReverseOrder() {
        Trade t = newTrade();
        gateway.failNext(100, false);

        SettlementOutcome outcome = saga.run(t.id());

        assertThat(outcome.state()).isEqualTo(SagaState.COMPENSATED);
        assertThat(journal.entries()).containsExactly(
                "reserve-cash " + t.id(),
                "deliver-securities " + t.id(),
                "undo deliver-securities " + t.id(),      // newest first ...
                "undo reserve-cash " + t.id(),            // ... then the older one
                "saga " + outcome.sagaId() + " compensated");
        assertThat(depot.isDelivered(t.id())).isFalse();
        assertThat(ledger.isReserved(t.id())).isFalse();
    }

    @Test
    void whenTheFirstStepFailsThereIsNothingToUndo() {
        Trade t = newTrade();
        ledger.setAvailable(BigDecimal.ONE);

        SettlementOutcome outcome = saga.run(t.id());

        assertThat(outcome.state()).isEqualTo(SagaState.COMPENSATED);
        assertThat(journal.entries()).containsExactly("saga " + outcome.sagaId() + " compensated");
        assertThat(trades.get(t.id()).status()).isEqualTo(TradeStatus.FAILED);
    }

    @Test
    void aSagaInterruptedByACrashResumesFromTheSavedStep() {
        Trade t = newTrade();
        // An Error is not a RuntimeException, so the saga cannot catch it: the "process" dies inside step 3.
        doThrow(new AssertionError("process killed")).when(gatewayStep).execute(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> saga.run(t.id())).isInstanceOf(AssertionError.class);

        var interrupted = sagas.findByState(SagaState.RUNNING).stream()
                .filter(s -> s.getTradeId().equals(t.id())).findFirst().orElseThrow();
        assertThat(interrupted.getNextStep()).isEqualTo(2);          // steps 0 and 1 are saved as done
        assertThat(trades.get(t.id()).status()).isEqualTo(TradeStatus.NEW);

        reset(gatewayStep);                                          // "restart": the step works again
        SettlementOutcome outcome = saga.resume(interrupted.getId());

        assertThat(outcome.state()).isEqualTo(SagaState.COMPLETED);
        assertThat(trades.get(t.id()).status()).isEqualTo(TradeStatus.SETTLED);
        assertThat(journal.entries()).containsExactly(               // steps 0 and 1 were NOT repeated
                "reserve-cash " + t.id(), "deliver-securities " + t.id(), "confirm-with-gateway " + t.id());
    }
}
