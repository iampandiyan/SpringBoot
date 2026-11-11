package com.example.trade.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.trade.config.TradeProperties;
import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;

@Service   
public class TradeService {
    private  final TradeRepository repository;
    private final TradeProperties props;
    public TradeService(TradeRepository repository, TradeProperties props){
        this.repository=repository;
        this.props=props;
    }

    public Trade create(String isin,BigDecimal quantity, BigDecimal price){
        if (quantity.compareTo(props.maxQuantity()) > 0) {
            throw new BusinessRuleException("Quantity " + quantity + " exceeds limit " + props.maxQuantity());
        }
        return repository.save(new Trade(null,isin,quantity,price, TradeStatus.NEW));
    }

    public Trade get(Long id){
        return repository.findById(id).orElseThrow(()-> new TradeNotFoundException(id));

    }

    public List<Trade> list(TradeStatus status) {
        return repository.findAll().stream()
                .filter(t -> status == null || t.status() == status)
                .toList();
    }


   public Trade settle(Long id) {
        Trade t = get(id);
        if (t.status() == TradeStatus.SETTLED) {
            throw new TradeStateException("Trade " + id + " is already settled");
        }
        return repository.save(t.withStatus(TradeStatus.SETTLED));
    }

    public Trade settleViaSelfCall(Long id){
        return this.settle(id);
    }
}
