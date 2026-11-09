package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.domain.TradeStatus;
import com.example.trade.persistence.TradeEntity;
import com.example.trade.persistence.TradeJpaRepository;
import com.example.trade.service.AllocationReportService;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Proves the N+1 problem with a NUMBER instead of an opinion: we count the SQL statements Hibernate prepares.
@SpringBootTest(properties = {
        "spring.main.web-application-type=none",
        "spring.jpa.properties.hibernate.generate_statistics=true"})   // makes Hibernate count statements
class NPlusOneTest {

    @Autowired TradeJpaRepository trades;
    @Autowired AllocationReportService report;
    @Autowired EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void seed() {
        trades.deleteAll();                  // start every test from a known state: exactly 5 trades, 10 allocations
        for (int i = 0; i < 5; i++) {
            var t = new TradeEntity("US0378331005", BigDecimal.TEN, BigDecimal.ONE, TradeStatus.NEW);
            t.addAllocation("ACC-A", new BigDecimal("6"));
            t.addAllocation("ACC-B", new BigDecimal("4"));
            trades.save(t);                  // cascade saves the two allocations too
        }
        stats = emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void naiveLoopRunsOneQueryPerTradePlusOne() {
        long tradeCount = trades.count();   // 5
        stats.clear();

        int total = report.totalAllocationsNaive();

        assertThat(total).isEqualTo((int) tradeCount * 2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1 + tradeCount);   // the "N+1"
    }

    @Test
    void joinFetchRunsExactlyOneQuery() {
        long tradeCount = trades.count();   // 5
        stats.clear();

        int total = report.totalAllocationsFetched();

        assertThat(total).isEqualTo((int) tradeCount * 2);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }
}
