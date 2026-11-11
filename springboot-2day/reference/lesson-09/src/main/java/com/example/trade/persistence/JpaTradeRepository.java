package com.example.trade.persistence;

import com.example.trade.domain.Trade;
import com.example.trade.repo.TradeRepository;
import com.example.trade.service.TradeNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

// ADAPTER: implements the TradeRepository interface the service already depends on, using Spring Data underneath.
// Because the service only knows the interface, swapping in-memory for JPA needed no change in the service's logic.
@Repository
public class JpaTradeRepository implements TradeRepository {

    private final TradeJpaRepository jpa;

    public JpaTradeRepository(TradeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Trade save(Trade t) {
        TradeEntity entity = (t.id() == null)
                ? new TradeEntity()
                : jpa.findById(t.id()).orElseThrow(() -> new TradeNotFoundException(t.id()));
        entity.apply(t);
        return jpa.save(entity).toDomain();
    }

    @Override
    public Optional<Trade> findById(Long id) {
        return jpa.findById(id).map(TradeEntity::toDomain);
    }

    @Override
    public List<Trade> findAll() {
        return jpa.findAll().stream().map(TradeEntity::toDomain).toList();
    }
}
