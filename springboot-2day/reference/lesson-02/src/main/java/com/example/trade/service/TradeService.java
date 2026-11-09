package com.example.trade.service;

import com.example.trade.config.TradeProperties;
import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private final TradeRepository repository;
    private final TradeProperties props;

    // Constructor injection: single constructor, so no @Autowired needed (since Spring 4.3).
    // The fields are final, so the service can never exist half-wired, and unit tests can call new TradeService(fake, props).
    public TradeService(TradeRepository repository, TradeProperties props) {
        this.repository = repository;
        this.props = props;
    }

    public Trade create(String isin, BigDecimal quantity, BigDecimal price) {
        // The limit comes from configuration (trade.max-quantity), so each environment can set its own.
        if (quantity.compareTo(props.maxQuantity()) > 0) {
            throw new IllegalArgumentException("Quantity " + quantity + " exceeds limit " + props.maxQuantity());
        }
        return repository.save(new Trade(null, isin, quantity, price, TradeStatus.NEW));
    }

    // DEMO of the self-invocation trap: the inner this.settle(id) call goes straight to the real
    // object, NOT through the Spring proxy, so TimingAspect never sees it (and neither would @Transactional).
    public Trade settleViaSelfCall(Long id) {
        return this.settle(id);
    }

    public Trade settle(Long id) {
        Trade t = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found: " + id));
        return repository.save(t.withStatus(TradeStatus.SETTLED));
    }
}
