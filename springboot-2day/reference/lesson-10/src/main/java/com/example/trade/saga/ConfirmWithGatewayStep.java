package com.example.trade.saga;

import com.example.trade.domain.Trade;
import com.example.trade.gateway.ResilientGatewayClient;
import com.example.trade.gateway.SettlementConfirmation;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// The LAST step is the "pivot": once the gateway has the confirmation we cannot take it back, so nothing may fail
// after it. That is why it comes last, and why it has no real compensation.
// It reuses the resilient client from lesson 07: retries and the circuit breaker still apply inside the step.
@Component
@Order(3)
public class ConfirmWithGatewayStep implements SagaStep {

    private final ResilientGatewayClient gateway;
    private final SagaJournal journal;

    public ConfirmWithGatewayStep(ResilientGatewayClient gateway, SagaJournal journal) {
        this.gateway = gateway;
        this.journal = journal;
    }

    @Override
    public String name() { return "confirm-with-gateway"; }

    @Override
    public void execute(Trade trade) {
        SettlementConfirmation result = gateway.confirm(trade);
        if (!result.confirmed()) {
            // The fallback answered "queued". For a saga that means "not done": the earlier steps must be undone.
            throw new SagaStepException("gateway did not confirm: " + result.note());
        }
        journal.add("confirm-with-gateway " + trade.id());
    }

    @Override
    public void compensate(Trade trade) {
        // Nothing to undo: if this step failed, no confirmation was sent; if it succeeded, it is never compensated.
        journal.add("undo confirm-with-gateway " + trade.id() + " (nothing to do)");
    }
}
