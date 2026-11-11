package com.example.trade.service;

// Unchecked on purpose: callers cannot sensibly recover, and unchecked exceptions roll back @Transactional (lesson 04).
// The web layer maps it to HTTP 404 in ApiExceptionHandler. The service knows nothing about HTTP.
public class TradeNotFoundException extends RuntimeException {
    public TradeNotFoundException(Long id) {
        super("Trade not found: " + id);
    }
}
