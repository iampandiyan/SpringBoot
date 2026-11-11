package com.example.trade.report;

import com.example.trade.events.TradeSettledEvent;
import com.example.trade.outbox.EventHandler;
import com.example.trade.outbox.OutboxMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// The PROJECTOR: listens to events and keeps the read model up to date. This is the only writer of the report table.
// Idempotent by design: the row key is the trade id, so handling the same event twice leaves one identical row.
@Component
public class TradeReportProjector implements EventHandler {

    private final SettledTradeReportRepository reports;
    private final ObjectMapper json;

    public TradeReportProjector(SettledTradeReportRepository reports, ObjectMapper json) {
        this.reports = reports;
        this.json = json;
    }

    @Override
    public boolean handles(String eventType) {
        return TradeSettledEvent.TYPE.equals(eventType);
    }

    @Override
    @Transactional
    public void handle(OutboxMessage message) {
        try {
            TradeSettledEvent e = json.readValue(message.payload(), TradeSettledEvent.class);
            reports.save(new SettledTradeReportEntity(e.tradeId(), e.isin(), e.quantity(),
                    e.quantity().multiply(e.price()), e.settledAt()));      // save() = insert or update by id
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot read event " + message.id(), ex);
        }
    }
}
