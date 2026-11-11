package com.example.trade;

import com.example.trade.persistence.JpaTradeRepository;
import com.example.trade.repo.TradeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// A persistence "slice": starts only JPA and an embedded database, not the web layer or our services.
// Each test runs in a transaction that is rolled back at the end, so tests do not affect each other.
// Since lesson 06 the tests themselves live in TradeRepositoryContract: the JPA adapter must pass the same ones.
@DataJpaTest
@Import(JpaTradeRepository.class)       // the slice scans only JPA repositories, so we add our adapter by hand
class JpaTradeRepositoryTest extends TradeRepositoryContract {

    @Autowired JpaTradeRepository repository;

    @Override
    TradeRepository repository() {
        return repository;
    }
}
