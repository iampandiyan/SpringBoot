package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.trade.config.TradeProperties;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.InMemoryTradeRepository;
import com.example.trade.service.BusinessRuleException;
import com.example.trade.service.TradeNotFoundException;
import com.example.trade.service.TradeService;
import com.example.trade.service.TradeStateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

// Plain unit test: NO Spring needed, which is the payoff of constructor injection.
class TradeServiceTest {
    private final TradeService svc = new TradeService(
            new InMemoryTradeRepository(), new TradeProperties(new BigDecimal("1000"), "TEST"));

    @Test
    void createThenSettle() {
        var t = svc.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        assertThat(t.status()).isEqualTo(TradeStatus.NEW);
        assertThat(svc.settle(t.id()).status()).isEqualTo(TradeStatus.SETTLED);
    }

    @Test
    void rejectsQuantityAboveConfiguredLimit() {
        assertThatThrownBy(() -> svc.create("US0378331005", new BigDecimal("1001"), BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds limit 1000");
    }

    @Test
    void unknownIdIsNotFound() {
        assertThatThrownBy(() -> svc.get(99L)).isInstanceOf(TradeNotFoundException.class);
    }

    @Test
    void settlingTwiceIsAStateError() {
        var t = svc.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        svc.settle(t.id());
        assertThatThrownBy(() -> svc.settle(t.id())).isInstanceOf(TradeStateException.class);
    }

    @Test
    void listFiltersByStatus() {
        var a = svc.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        svc.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        svc.settle(a.id());
        assertThat(svc.list(null)).hasSize(2);
        assertThat(svc.list(TradeStatus.SETTLED)).extracting("id").containsExactly(a.id());
        assertThat(svc.list(TradeStatus.NEW)).hasSize(1);
    }
}
