package com.example.trade.saga;

import com.example.trade.domain.Trade;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)                                          // steps run in @Order sequence
public class ReserveCashStep implements SagaStep {

    private final CashLedger ledger;
    private final SagaJournal journal;

    public ReserveCashStep(CashLedger ledger, SagaJournal journal) {
        this.ledger = ledger;
        this.journal = journal;
    }

    @Override
    public String name() { return "reserve-cash"; }

    @Override
    public void execute(Trade trade) {
        ledger.reserve(trade.id(), trade.quantity().multiply(trade.price()));
        journal.add("reserve-cash " + trade.id());
    }

    @Override
    public void compensate(Trade trade) {
        ledger.release(trade.id());
        journal.add("undo reserve-cash " + trade.id());
    }
}
