package com.example.trade.service;

import com.example.trade.persistence.TradeJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// A read-only report. It talks to the JPA repository directly (reports often skip the domain model).
// @Transactional is REQUIRED here: lazy collections can only be loaded while the Hibernate session is open.
@Service
@Transactional(readOnly = true)
public class AllocationReportService {

    private final TradeJpaRepository trades;

    public AllocationReportService(TradeJpaRepository trades) {
        this.trades = trades;
    }

    // N+1: 1 query for the trades, then 1 more query PER TRADE the first time .getAllocations() is touched.
    public int totalAllocationsNaive() {
        return trades.findAll().stream().mapToInt(t -> t.getAllocations().size()).sum();
    }

    // The fix: one JOIN FETCH query, nothing left to load lazily.
    public int totalAllocationsFetched() {
        return trades.findAllWithAllocations().stream().mapToInt(t -> t.getAllocations().size()).sum();
    }
}
