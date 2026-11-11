package com.example.trade.domain;

import java.math.BigDecimal;

public record Trade(Long id, String isin, BigDecimal quantity, BigDecimal price, TradeStatus status) {
    public Trade withStatus(TradeStatus s){
        return new Trade(id, isin, quantity, price, s);
    }
}
