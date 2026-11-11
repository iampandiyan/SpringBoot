package com.example.trade.repo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.example.trade.domain.Trade;

@Repository 
public class InMemoryTradeRepository implements TradeRepository {
    private final Map<Long, Trade> store=new ConcurrentHashMap<>();
    private final AtomicLong seq=new AtomicLong();

    @Override
    public Trade save(Trade t) {
        Long id=t.id()!=null?t.id():seq.incrementAndGet();
        Trade saved=new Trade(id,t.isin(),t.quantity(),t.price(),t.status());
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Trade> findById(Long id) {        
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Trade> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Trade::id)).toList();
    }
}
