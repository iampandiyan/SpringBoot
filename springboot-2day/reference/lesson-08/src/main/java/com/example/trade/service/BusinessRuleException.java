package com.example.trade.service;

// The request is well-formed but breaks a business rule (for example the quantity limit). Mapped to HTTP 422.
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
