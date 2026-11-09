package com.example.trade.repo;

import com.example.trade.domain.Trade;
import java.util.Optional;

// Depend on the INTERFACE. Spring injects whichever implementation bean exists.
// (Day 1 file 04 replaces the in-memory version with JPA, with no service changes.)
public interface TradeRepository {
    Trade save(Trade t);
    Optional<Trade> findById(Long id);
}
