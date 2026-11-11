package com.example.trade.events;

import java.math.BigDecimal;
import java.time.Instant;

// A FACT that already happened, named in the past tense. It carries everything a consumer needs, so consumers
// never have to call back to ask. Events are a public contract: add fields, never rename or remove them.
public record TradeSettledEvent(Long tradeId, String isin, BigDecimal quantity, BigDecimal price, Instant settledAt) {
    public static final String TYPE = "TradeSettled";
}
