package com.example.trade.repo;

import java.util.Optional;

import com.example.trade.domain.Trade;

public interface TradeRepository {
    Trade save(Trade t);
    Optional<Trade> findById(Long id);  
}
