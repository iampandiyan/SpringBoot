package com.example.trade.saga;

import com.example.trade.domain.Trade;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class DeliverSecuritiesStep implements SagaStep {

    private final SecuritiesDepot depot;
    private final SagaJournal journal;

    public DeliverSecuritiesStep(SecuritiesDepot depot, SagaJournal journal) {
        this.depot = depot;
        this.journal = journal;
    }

    @Override
    public String name() { return "deliver-securities"; }

    @Override
    public void execute(Trade trade) {
        depot.deliver(trade.id(), trade.isin());
        journal.add("deliver-securities " + trade.id());
    }

    @Override
    public void compensate(Trade trade) {
        depot.takeBack(trade.id());
        journal.add("undo deliver-securities " + trade.id());
    }
}
