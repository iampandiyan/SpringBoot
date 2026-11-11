package com.example.trade.repo;

import java.util.List;
import java.util.Optional;

import com.example.trade.domain.Trade;

public interface TradeRepository {
    Trade save(Trade t);
    Optional<Trade> findById(Long id);  
    List <Trade> findAll();
}
