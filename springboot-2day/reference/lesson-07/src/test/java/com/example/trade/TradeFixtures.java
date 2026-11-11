package com.example.trade;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import java.math.BigDecimal;

// Test data in ONE place. When the Trade record gains a field, only this file changes, not fifty tests.
final class TradeFixtures {
    private TradeFixtures() {}

    static final String ISIN = "US0378331005";

    /** A valid, unsaved trade (no id yet). */
    static Trade newTrade() {
        return new Trade(null, ISIN, new BigDecimal("100"), new BigDecimal("189.50"), TradeStatus.NEW);
    }

    /** The same trade as the database would return it, with an id. */
    static Trade savedTrade(long id, TradeStatus status) {
        return new Trade(id, ISIN, new BigDecimal("100"), new BigDecimal("189.50"), status);
    }
}
