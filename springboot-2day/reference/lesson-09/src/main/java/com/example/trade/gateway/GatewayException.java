package com.example.trade.gateway;

// A TRANSIENT failure: timeout, connection reset, 503 from the gateway. Retrying may succeed.
public class GatewayException extends RuntimeException {
    public GatewayException(String message) {
        super(message);
    }
}
