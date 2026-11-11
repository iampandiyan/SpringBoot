package com.example.trade.saga;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

// Stand-in for the securities depot (another service). It can be told to refuse a delivery, to make the saga fail.
@Component
public class SecuritiesDepot {

    private final Set<Long> delivered = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean refuse = new AtomicBoolean();

    public void deliver(Long tradeId, String isin) {
        if (refuse.get()) {
            throw new SagaStepException("depot refused delivery of " + isin);
        }
        delivered.add(tradeId);
    }

    public void takeBack(Long tradeId) {
        delivered.remove(tradeId);
    }

    public boolean isDelivered(Long tradeId) { return delivered.contains(tradeId); }

    public void refuseDeliveries(boolean value) { refuse.set(value); }

    public void reset() {
        delivered.clear();
        refuse.set(false);
    }
}
