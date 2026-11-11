package com.example.trade.gateway;

import com.example.trade.domain.Trade;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Every call to the remote gateway goes through here. The three annotations are proxy-based (lesson 01), so this class
// must be called from ANOTHER bean (ConfirmationService), never with this.confirm().
//
// How Resilience4j nests them (fixed order, outermost first):   Retry( CircuitBreaker( Bulkhead( the call ) ) )
//   - Bulkhead: caps concurrent calls, so a slow gateway cannot use up all our threads.
//   - CircuitBreaker: records each attempt; once too many fail it OPENS and refuses calls instantly.
//   - Retry: repeats a failed attempt a few times, with a pause.
// The fallback sits on the OUTERMOST annotation (@Retry). A fallback on the inner @CircuitBreaker would turn every
// failure into a normal return value, and the Retry outside it would never see an exception to retry.
@Service
public class ResilientGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientGatewayClient.class);

    private final SettlementGateway gateway;

    public ResilientGatewayClient(SettlementGateway gateway) {
        this.gateway = gateway;
    }

    @Retry(name = "gateway", fallbackMethod = "gatewayDown")
    @CircuitBreaker(name = "gateway")
    @Bulkhead(name = "gateway")
    public SettlementConfirmation confirm(Trade trade) {
        // The trade id is the idempotency key: a retry (or a second click) can never deliver twice.
        return gateway.send("T" + trade.id(), trade);
    }

    // Fallbacks share ONE name (the one in fallbackMethod) and take the same parameters plus an exception. Resilience4j
    // picks the overload whose exception type matches best.
    // GatewayRejectedException has NO fallback on purpose: a permanent "no" must reach the caller as an error.
    SettlementConfirmation gatewayDown(Trade trade, GatewayException e) {
        log.warn("Gateway unavailable for trade {} after retries: {}", trade.id(), e.getMessage());
        return SettlementConfirmation.unconfirmed("T" + trade.id(), "gateway unavailable, queued for later");
    }

    SettlementConfirmation gatewayDown(Trade trade, CallNotPermittedException e) {
        log.warn("Circuit open, call for trade {} not attempted", trade.id());
        return SettlementConfirmation.unconfirmed("T" + trade.id(), "circuit open, queued for later");
    }

    SettlementConfirmation gatewayDown(Trade trade, BulkheadFullException e) {
        log.warn("Bulkhead full, call for trade {} not attempted", trade.id());
        return SettlementConfirmation.unconfirmed("T" + trade.id(), "too many calls in flight, queued for later");
    }
}
