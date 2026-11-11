package com.example.trade.gateway;

import com.example.trade.domain.Trade;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

// A simulator of an unreliable remote gateway, so we can PROVE the resilience behaviour on a laptop.
// It also behaves like a well-built gateway: the same idempotency key is delivered only once.
@Component
public class FlakySettlementGateway implements SettlementGateway {

    private final AtomicInteger attempts = new AtomicInteger();          // every call, successful or not
    private final AtomicInteger failuresToInject = new AtomicInteger();  // how many upcoming calls should fail
    private final Set<String> delivered = ConcurrentHashMap.newKeySet(); // keys the gateway really processed
    private volatile boolean loseReplies;                                // fail AFTER processing: the reply is lost
    private volatile boolean rejectAll;
    private volatile CountDownLatch holdCalls;                           // lets a test keep calls "in flight"
    private volatile CountDownLatch entered = new CountDownLatch(0);

    @Override
    public SettlementConfirmation send(String idempotencyKey, Trade trade) {
        attempts.incrementAndGet();
        entered.countDown();
        awaitIfHeld();
        if (rejectAll) {
            throw new GatewayRejectedException("Unknown counterparty for " + trade.isin());
        }
        if (failuresToInject.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0) {
            if (loseReplies) {
                delivered.add(idempotencyKey);      // the message arrived, but the answer never came back
            }
            throw new GatewayException("gateway timeout");
        }
        boolean firstTime = delivered.add(idempotencyKey);
        return SettlementConfirmation.confirmed(idempotencyKey, "SWIFT-" + idempotencyKey,
                firstTime ? "delivered" : "duplicate ignored");
    }

    private void awaitIfHeld() {
        CountDownLatch latch = holdCalls;
        if (latch != null) {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ---- controls used by the demo and the tests ----
    public void failNext(int calls, boolean replyLost) {
        failuresToInject.set(calls);
        loseReplies = replyLost;
    }

    public void rejectEverything(boolean value) { rejectAll = value; }

    /** Calls block until release(); expectCalls tells awaitEntered() how many callers to wait for. */
    public void hold(int expectCalls) {
        holdCalls = new CountDownLatch(1);
        entered = new CountDownLatch(expectCalls);
    }

    public boolean awaitEntered() throws InterruptedException { return entered.await(5, TimeUnit.SECONDS); }

    public void release() {
        CountDownLatch latch = holdCalls;
        if (latch != null) {
            latch.countDown();
        }
        holdCalls = null;
    }

    public int attempts() { return attempts.get(); }

    public int distinctDeliveries() { return delivered.size(); }

    public void reset() {
        release();
        attempts.set(0);
        failuresToInject.set(0);
        delivered.clear();
        loseReplies = false;
        rejectAll = false;
        entered = new CountDownLatch(0);
    }
}
