package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.persistence.JpaTradeRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// A persistence "slice": starts only JPA and an embedded database, not the web layer or our services.
// Each test runs in a transaction that is rolled back at the end, so tests do not affect each other.
@DataJpaTest
@Import(JpaTradeRepository.class)       // the slice scans only JPA repositories, so we add our adapter by hand
class JpaTradeRepositoryTest {

    @Autowired JpaTradeRepository repository;

    @Test
    void saveAssignsAnIdAndFindByIdReadsItBack() {
        Trade saved = repository.save(new Trade(null, "US0378331005", new BigDecimal("100"), new BigDecimal("189.5"), TradeStatus.NEW));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findById(saved.id())).hasValueSatisfying(t -> {
            assertThat(t.isin()).isEqualTo("US0378331005");
            assertThat(t.price()).isEqualByComparingTo("189.5");   // compare BigDecimal by value, not scale
        });
    }

    @Test
    void savingWithAnExistingIdUpdatesTheRow() {
        Trade saved = repository.save(new Trade(null, "US0378331005", BigDecimal.TEN, BigDecimal.ONE, TradeStatus.NEW));

        repository.save(saved.withStatus(TradeStatus.SETTLED));

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(TradeStatus.SETTLED);
        assertThat(repository.findAll()).hasSize(1);
    }
}
