package com.example.trade.events;

public record TradeSettlementFailedEvent(Long tradeId, String reason) {
    public static final String TYPE = "TradeSettlementFailed";
}
