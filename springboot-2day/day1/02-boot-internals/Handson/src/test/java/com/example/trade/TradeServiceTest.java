package com.example.trade;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.trade.config.TradeProperties;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.InMemoryTradeRepository;
import com.example.trade.service.TradeService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class  TradeServiceTest{

    private final TradeService svc = new TradeService(
            new InMemoryTradeRepository(), new TradeProperties(new BigDecimal("1000"), "TEST"));
    @Test 
    void createThenSettle(){
        var svc=new TradeService(new InMemoryTradeRepository(), new TradeProperties(new BigDecimal(1000), "TEST"));
        var t=svc.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        assertThat(t.status()).isEqualTo(TradeStatus.NEW);
        assertThat(svc.settle(t.id()).status()).isEqualTo(TradeStatus.SETTLED);
}

@Test 
void rejectsQuantityAboveConfiguredLimit(){
     assertThatThrownBy(() -> svc.create("US0378331005", new BigDecimal("1001"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity exceeds maximum allowed");
}
}