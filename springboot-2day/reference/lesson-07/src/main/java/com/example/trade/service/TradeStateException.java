package com.example.trade.service;

// The trade exists but is in the wrong state for this action (settling a trade that is already settled).
// Mapped to HTTP 409 Conflict.
public class TradeStateException extends RuntimeException {
    public TradeStateException(String message) {
        super(message);
    }
}
