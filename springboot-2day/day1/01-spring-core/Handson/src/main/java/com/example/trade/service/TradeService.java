package com.example.trade.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;

@Service   
public class TradeService {
    private  final TradeRepository repository;
    public TradeService(TradeRepository repository){
        this.repository=repository;
    }

    public Trade create(String isin,BigDecimal quantity, BigDecimal price){
        return repository.save(new Trade(null,isin,quantity,price, TradeStatus.NEW));
    }

    public Trade settle(Long id){
        Trade t=repository.findById(id).orElseThrow(()-> new IllegalArgumentException("Trade not found: "+id));
        return repository.save(t.withStatus(TradeStatus.SETTLED));
    }

    public Trade settleViaSelfCall(Long id){
        return this.settle(id);
    }
}
