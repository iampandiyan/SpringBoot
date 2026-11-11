package com.example.trade.service;

public class TradeNotFoundException extends RuntimeException {
    public TradeNotFoundException(Long id) {
        super("Trade not found: " + id);
    }
}
