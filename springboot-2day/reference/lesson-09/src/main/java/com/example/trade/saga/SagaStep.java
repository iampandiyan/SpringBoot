package com.example.trade.saga;

import com.example.trade.domain.Trade;

// One local step of a saga, and the action that undoes its effect. There is no distributed transaction: if a later step
// fails, the orchestrator calls compensate() on the steps that already succeeded, in reverse order.
// Both methods must be IDEMPOTENT: after a crash they may run again.
public interface SagaStep {
    String name();

    void execute(Trade trade);

    void compensate(Trade trade);
}
