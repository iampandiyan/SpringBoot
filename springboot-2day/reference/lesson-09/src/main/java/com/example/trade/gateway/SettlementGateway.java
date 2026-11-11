package com.example.trade.gateway;

import com.example.trade.domain.Trade;

// The remote system we depend on (think: a SWIFT/T2S gateway). Production code would implement this with an HTTP or
// MQ client. The idempotency key lets the gateway recognise a repeated request, which is what makes retrying safe.
public interface SettlementGateway {
    SettlementConfirmation send(String idempotencyKey, Trade trade);
}
