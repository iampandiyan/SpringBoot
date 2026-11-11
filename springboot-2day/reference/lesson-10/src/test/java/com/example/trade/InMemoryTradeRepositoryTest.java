package com.example.trade;

import com.example.trade.repo.InMemoryTradeRepository;
import com.example.trade.repo.TradeRepository;

// The fake passes the same contract as the real thing, which is what makes it a trustworthy fake for unit tests.
class InMemoryTradeRepositoryTest extends TradeRepositoryContract {

    // JUnit creates a NEW test-class instance for every test method, so each test gets an empty repository.
    private final InMemoryTradeRepository repository = new InMemoryTradeRepository();

    @Override
    TradeRepository repository() {
        return repository;
    }
}
