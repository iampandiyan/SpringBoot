package com.example.trade.gateway;

// The outcome of asking the gateway to confirm a trade.
// confirmed = false does not mean "failed": it means "we could not reach the gateway; the request is still owed".
public record SettlementConfirmation(String tradeKey, boolean confirmed, String reference, String note) {
    public static SettlementConfirmation confirmed(String tradeKey, String reference, String note) {
        return new SettlementConfirmation(tradeKey, true, reference, note);
    }

    public static SettlementConfirmation unconfirmed(String tradeKey, String reason) {
        return new SettlementConfirmation(tradeKey, false, null, reason);
    }
}
