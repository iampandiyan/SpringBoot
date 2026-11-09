package com.example.trade.service;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private final TradeRepository repository;

    // Constructor injection: single constructor, so no @Autowired needed (since Spring 4.3).
    // The field is final, so the service can never exist half-wired, and unit tests can call new TradeService(fake).
    public TradeService(TradeRepository repository) {
        this.repository = repository;
    }

    public Trade create(String isin, BigDecimal quantity, BigDecimal price) {
        return repository.save(new Trade(null, isin, quantity, price, TradeStatus.NEW));
    }

    public Trade settle(Long id) {
        Trade t = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + id));
        return repository.save(t.withStatus(TradeStatus.SETTLED));
    }
}
