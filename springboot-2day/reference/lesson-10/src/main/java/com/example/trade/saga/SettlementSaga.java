package com.example.trade.saga;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.service.TradeNotFoundException;
import com.example.trade.service.TradeService;
import com.example.trade.service.TradeStateException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// The ORCHESTRATOR: one place that knows the order of the steps, runs them, and undoes them on failure.
// It is deliberately NOT @Transactional. A saga exists because no single transaction can span several systems.
// Each step commits its own local change; the saga's progress is saved to the database after every step.
@Service
public class SettlementSaga {

    private static final Logger log = LoggerFactory.getLogger(SettlementSaga.class);

    private final List<SagaStep> steps;            // Spring injects them sorted by @Order
    private final SagaInstanceRepository sagas;
    private final TradeService trades;
    private final SagaJournal journal;

    public SettlementSaga(List<SagaStep> steps, SagaInstanceRepository sagas, TradeService trades, SagaJournal journal) {
        this.steps = steps;
        this.sagas = sagas;
        this.trades = trades;
        this.journal = journal;
    }

    public SettlementOutcome run(Long tradeId) {
        Trade trade = trades.get(tradeId);
        if (trade.status() != TradeStatus.NEW) {
            throw new TradeStateException("Trade " + tradeId + " is " + trade.status() + "; only NEW trades can be settled");
        }
        SagaInstanceEntity saga = sagas.save(new SagaInstanceEntity(tradeId));
        return proceed(saga, trade);
    }

    /** What a recovery job calls for a saga found RUNNING after a restart: continue from the saved step. */
    public SettlementOutcome resume(Long sagaId) {
        SagaInstanceEntity saga = sagas.findById(sagaId)
                .orElseThrow(() -> new TradeNotFoundException(sagaId));
        if (saga.getState() != SagaState.RUNNING) {
            return outcome(saga);
        }
        return proceed(saga, trades.get(saga.getTradeId()));
    }

    private SettlementOutcome proceed(SagaInstanceEntity saga, Trade trade) {
        for (int i = saga.getNextStep(); i < steps.size(); i++) {
            SagaStep step = steps.get(i);
            try {
                step.execute(trade);
            } catch (RuntimeException e) {
                log.warn("saga {} failed at step '{}': {}", saga.getId(), step.name(), e.getMessage());
                return rollBack(saga, trade, i, step.name() + ": " + e.getMessage());
            }
            saga.stepDone();
            sagas.save(saga);                       // progress is durable before the next step starts
        }
        // Every step worked. Settle the trade (and write the event) once; a resumed saga may find it already done.
        if (trades.get(trade.id()).status() != TradeStatus.SETTLED) {
            trades.settle(trade.id());
        }
        saga.completed();
        sagas.save(saga);
        return outcome(saga);
    }

    // Undo the steps that succeeded, newest first. The failed step itself is responsible for leaving nothing behind.
    private SettlementOutcome rollBack(SagaInstanceEntity saga, Trade trade, int failedIndex, String reason) {
        for (int i = failedIndex - 1; i >= 0; i--) {
            steps.get(i).compensate(trade);
        }
        trades.fail(trade.id(), reason);
        saga.compensated(reason);
        sagas.save(saga);
        journal.add("saga " + saga.getId() + " compensated");
        return outcome(saga);
    }

    private SettlementOutcome outcome(SagaInstanceEntity saga) {
        return new SettlementOutcome(saga.getId(), saga.getTradeId(), saga.getState(), saga.getFailureReason());
    }
}
