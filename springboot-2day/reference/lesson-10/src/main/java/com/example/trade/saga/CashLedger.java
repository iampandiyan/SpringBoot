package com.example.trade.saga;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// Stand-in for ANOTHER service (the cash account system). In production this would be an HTTP or messaging call.
@Component
public class CashLedger {

    private final Map<Long, BigDecimal> reserved = new ConcurrentHashMap<>();
    private volatile BigDecimal available = new BigDecimal("1000000000");

    public void reserve(Long tradeId, BigDecimal amount) {
        if (amount.compareTo(available) > 0) {
            throw new SagaStepException("insufficient cash: need " + amount + ", available " + available);
        }
        reserved.put(tradeId, amount);                 // same trade, same amount: repeating it changes nothing
    }

    public void release(Long tradeId) {
        reserved.remove(tradeId);                      // releasing nothing is fine: idempotent
    }

    public boolean isReserved(Long tradeId) { return reserved.containsKey(tradeId); }

    public void setAvailable(BigDecimal value) { available = value; }

    public void reset() {
        reserved.clear();
        available = new BigDecimal("1000000000");
    }
}
