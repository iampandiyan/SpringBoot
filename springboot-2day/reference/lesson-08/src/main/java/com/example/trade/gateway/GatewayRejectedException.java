package com.example.trade.gateway;

// A PERMANENT answer: the gateway understood the message and said no (invalid BIC, unknown account...).
// Retrying cannot help, and it says nothing about the gateway's health, so the retry and the circuit breaker ignore it.
public class GatewayRejectedException extends RuntimeException {
    public GatewayRejectedException(String message) {
        super(message);
    }
}
