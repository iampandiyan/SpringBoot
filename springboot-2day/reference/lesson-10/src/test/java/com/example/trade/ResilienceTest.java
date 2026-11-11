package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.gateway.FlakySettlementGateway;
import com.example.trade.gateway.GatewayRejectedException;
import com.example.trade.gateway.ResilientGatewayClient;
import com.example.trade.gateway.SettlementConfirmation;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// The real Spring context (so the Resilience4j proxies are real), no web server. We shorten the waits and the window so
// the tests are fast, and use the simulator gateway to make the "remote system" fail on demand.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "resilience4j.retry.instances.gateway.wait-duration=10ms",
        "resilience4j.circuitbreaker.instances.gateway.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.gateway.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.gateway.permitted-number-of-calls-in-half-open-state=2",
        "resilience4j.circuitbreaker.instances.gateway.wait-duration-in-open-state=1h",
        "resilience4j.bulkhead.instances.gateway.max-concurrent-calls=2"})
class ResilienceTest {

    @Autowired ResilientGatewayClient client;
    @Autowired FlakySettlementGateway gateway;
    @Autowired CircuitBreakerRegistry breakers;

    private final Trade trade = new Trade(7L, "US0378331005", new BigDecimal("100"), new BigDecimal("189.50"), TradeStatus.NEW);

    private CircuitBreaker breaker() {
        return breakers.circuitBreaker("gateway");
    }

    @BeforeEach
    @AfterEach
    void cleanState() {
        gateway.reset();
        breaker().reset();
    }

    @Test
    void retryHidesTwoTransientFailures() {
        gateway.failNext(2, false);

        SettlementConfirmation result = client.confirm(trade);

        assertThat(result.confirmed()).isTrue();
        assertThat(gateway.attempts()).isEqualTo(3);     // fail, fail, success
    }

    @Test
    void whenRetriesRunOutTheFallbackAnswersInsteadOfAnException() {
        gateway.failNext(100, false);

        SettlementConfirmation result = client.confirm(trade);

        assertThat(result.confirmed()).isFalse();
        assertThat(result.note()).contains("gateway unavailable");
        assertThat(gateway.attempts()).isEqualTo(3);     // max-attempts, not more
    }

    @Test
    void aLostReplyIsSafeToRetryBecauseTheKeyIsIdempotent() {
        gateway.failNext(1, true);                       // the gateway processed it, but the reply was lost

        SettlementConfirmation result = client.confirm(trade);

        assertThat(result.confirmed()).isTrue();
        assertThat(result.note()).isEqualTo("duplicate ignored");   // the retry was recognised as a repeat
        assertThat(gateway.attempts()).isEqualTo(2);
        assertThat(gateway.distinctDeliveries()).isEqualTo(1);      // delivered once, not twice
    }

    @Test
    void aPermanentRejectionIsNeitherRetriedNorHiddenByTheFallback() {
        gateway.rejectEverything(true);

        assertThatThrownBy(() -> client.confirm(trade)).isInstanceOf(GatewayRejectedException.class);

        assertThat(gateway.attempts()).isEqualTo(1);     // no retry
        assertThat(breaker().getMetrics().getNumberOfFailedCalls()).isZero();   // not counted against the gateway
    }

    @Test
    void theCircuitOpensAfterEnoughFailuresAndThenRefusesCallsWithoutTouchingTheGateway() {
        gateway.failNext(1000, false);

        client.confirm(trade);                           // 3 failed attempts recorded
        SettlementConfirmation second = client.confirm(trade);   // the 4th failure opens the circuit; the next attempt is refused

        assertThat(breaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(second.note()).contains("circuit open");
        assertThat(gateway.attempts()).isEqualTo(4);

        SettlementConfirmation third = client.confirm(trade);
        assertThat(third.note()).contains("circuit open");
        assertThat(gateway.attempts()).isEqualTo(4);     // unchanged: the open circuit protected the gateway
    }

    @Test
    void afterTheProbeCallsSucceedTheCircuitClosesAgain() {
        breaker().transitionToOpenState();
        breaker().transitionToHalfOpenState();           // what the timer does after wait-duration-in-open-state
        gateway.failNext(0, false);

        client.confirm(trade);
        client.confirm(trade);                           // permitted-number-of-calls-in-half-open-state = 2 successes

        assertThat(breaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void theBulkheadRefusesCallsAboveTheLimitWhileTwoAreInFlight() throws Exception {
        gateway.hold(2);                                 // the first two callers will block inside the gateway
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<SettlementConfirmation> a = CompletableFuture.supplyAsync(() -> client.confirm(trade), pool);
            CompletableFuture<SettlementConfirmation> b = CompletableFuture.supplyAsync(() -> client.confirm(trade), pool);
            assertThat(gateway.awaitEntered()).isTrue(); // both are now in flight

            SettlementConfirmation third = client.confirm(trade);   // no room left

            assertThat(third.confirmed()).isFalse();
            assertThat(third.note()).contains("too many calls");
            gateway.release();
            assertThat(a.get().confirmed()).isTrue();
            assertThat(b.get().confirmed()).isTrue();
        } finally {
            gateway.release();
            pool.shutdownNow();
        }
    }
}
